import { RefinedResponse } from "k6/http";
import {
  authenticateWeb,
  Structure,
  UserInfo,
  ProfileGroup,
  getHeaders,
  getAllStructures,
  getUsersOfSchool,
  getClassesOfStructureOrFail,
  getProfileGroupsOfStructure,
  getUsersOfGroup,
  assertOk,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import http from "k6/http";
import { check, group, sleep } from "k6";
import { AAFGeneratorBuilder, AAFGeneratedModel, AAFFiles } from "./_aaf-generator.ts";

const maxDuration = __ENV.MAX_DURATION || "20m";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const rootUrl = __ENV.ROOT_URL;
const schoolNameBig = __ENV.DATA_SCHOOL_NAME_BIG || "IT Directory Big";
const indexStart = parseInt(__ENV.INDEX_START || "500000");
const MAX_WAIT_BIG_AAF_IMPORTED = parseInt(__ENV.MAX_WAIT_BIG_AAF_IMPORTED || "300");

const NB_STRUCTURES = parseInt(__ENV.NB_STRUCTURES || "10");
const NB_CLASSES_PER_STRUCTURE = parseInt(__ENV.NB_CLASSES_PER_STRUCTURE || "3");
const NB_STUDENTS_PER_CLASS = parseInt(__ENV.NB_STUDENTS_PER_CLASS || "25");
const NB_TEACHERS_PER_STRUCTURE = parseInt(__ENV.NB_TEACHERS_PER_STRUCTURE || "8");
const NB_GROUPS_PER_STRUCTURE = parseInt(__ENV.NB_GROUPS_PER_STRUCTURE || "3");
const NB_STUDENTS_PER_GROUP = parseInt(__ENV.NB_STUDENTS_PER_GROUP || "10");

// [childStructureIdx, parentStructureIdx] pairs, to exercise ENTEtablissementStructRattachFctl / HAS_ATTACHMENT.
const STRUCTURE_ATTACHMENTS: [number, number][] = NB_STRUCTURES > 3 ? [[1, 0], [3, 2]] : [];

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testImportBigAAF: {
      executor: "per-vu-iterations",
      exec: "testImportAAFBig",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  model: AAFGeneratedModel;
  // both indexed like model.structures (by structure index)
  structures: Structure[];
  usersByStructure: UserInfo[][];
};

export function setup(): InitData {
  let result!: { structures: Structure[]; usersByStructure: UserInfo[][] };
  let model!: AAFGeneratedModel;

  group("[Directory] Generate and import a big multi-structure AAF dataset", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    model = generateAndImportBigAAF();
    result = waitForBigAAFImport(model);
  });
  console.log(`[Directory] Big AAF import setup completed: ${model.structures.length} structures, ` +
    `${model.students.length} students, ${model.relatives.length} relatives, ${model.teachers.length} teachers, ` +
    `${model.classes.length} classes, ${model.groups.length} groups`);
  return { model, structures: result.structures, usersByStructure: result.usersByStructure };
}

/*******************************************************************************************************
 *  Verification
 ******************************************************************************************************/
export function testImportAAFBig(data: InitData) {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  const { model, structures, usersByStructure } = data;

  group("[Directory][BigAAF] Structures", () => {
    check(structures, {
      "all generated structures were imported": (s) => s.length === model.structures.length,
      "all imported structures have an id": (s) => s.every((st) => !!st.id),
    });
  });

  group("[Directory][BigAAF] Users - students / teachers / relatives per structure", () => {
    model.structures.forEach((s) => {
      const users = usersByStructure[s.index];
      const expectedStudents = model.students.filter((st) => st.structureIndex === s.index).length;
      const expectedTeachers = model.teachers.filter((t) => t.structureIndex === s.index).length;
      const expectedRelatives = relativeIndicesOfStructure(model, s.index).size;
      check(users, {
        [`structure ${s.index} (${structures[s.index].name}) has expected student count`]:
          (u) => u.filter((x) => x.type === "Student").length === expectedStudents,
        [`structure ${s.index} (${structures[s.index].name}) has expected teacher count`]:
          (u) => u.filter((x) => x.type === "Teacher").length === expectedTeachers,
        [`structure ${s.index} (${structures[s.index].name}) has expected relative count`]:
          (u) => u.filter((x) => x.type === "Relative").length === expectedRelatives,
      });
    });
  });

  group("[Directory][BigAAF] Classes", () => {
    model.structures.forEach((s) => {
      const structure = structures[s.index];
      const users = usersByStructure[s.index];
      const expectedClasses = model.classes.filter((c) => c.structureIndex === s.index);
      const actualClasses = getClassesOfStructureOrFail(structure.id);

      check(actualClasses, {
        [`structure ${s.index} has expected number of classes`]: (c) => c.length === expectedClasses.length,
        [`structure ${s.index} classes have the expected names`]:
          (c) => expectedClasses.every((ec) => c.some((cl) => cl.name === ec.name)),
      });

      expectedClasses.forEach((ec) => {
        const expectedStudentCount = model.students.filter((st) => st.classIndex === ec.index).length;
        const actualStudentCount = users.filter(
          (u) => u.type === "Student" && u.classes && u.classes.some((cl) => cl.name === ec.name)
        ).length;
        check(
          { actualStudentCount },
          { [`class ${ec.name} of structure ${s.index} has expected student count`]: (r) => r.actualStudentCount === expectedStudentCount }
        );
      });
    });
  });

  group("[Directory][BigAAF] Functional groups", () => {
    model.structures.forEach((s) => {
      const structure = structures[s.index];
      const expectedGroups = model.groups.filter((g) => g.structureIndex === s.index);
      const adminGroups: ProfileGroup[] = getProfileGroupsOfStructure(structure.id);

      expectedGroups.forEach((eg) => {
        const found = adminGroups.find((g) => g.name === eg.name);
        check(found, {
          [`group ${eg.name} of structure ${s.index} exists`]: (g) => !!g,
          [`group ${eg.name} of structure ${s.index} has expected member count`]:
            (g) => !!g && g.nbUsers === eg.studentIndices.length,
        });

        // deep-dive: exact membership (by externalId), on the first structure only, to bound API calls.
        if (found && s.index === 0) {
          const members = getUsersOfGroup(found.id);
          const expectedExternalIds = new Set(eg.studentIndices.map((idx) => model.students[idx].externalId));
          const actualExternalIds = new Set(members.map((m) => m.externalId));
          check(members, {
            [`group ${eg.name} of structure 0 has the exact expected members`]:
              () => expectedExternalIds.size === actualExternalIds.size &&
                Array.from(expectedExternalIds).every((id) => actualExternalIds.has(id)),
          });
        }
      });
    });
  });

  group("[Directory][BigAAF] Structure attachments", () => {
    STRUCTURE_ATTACHMENTS.forEach(([childIdx, parentIdx]) => {
      const child = structures[childIdx];
      const parent = structures[parentIdx];
      check(child, {
        [`structure ${childIdx} (${child.name}) lists structure ${parentIdx} (${parent.name}) as a parent`]:
          (c) => (c.parents || []).some((p) => p.id === parent.id),
      });

      const childrenRes = http.get(`${rootUrl}/directory/structure/${parent.id}/children`, { headers: getHeaders() });
      check(childrenRes, {
        [`structure ${parentIdx} (${parent.name}) lists structure ${childIdx} (${child.name}) as a child`]: (r) => {
          const body = JSON.parse(<string>r.body);
          return Array.isArray(body) && body.some((c: any) => c.id === child.id);
        },
      });
    });
  });

  group("[Directory][BigAAF] Sampled teacher detail", () => {
    model.structures.forEach((s) => {
      const modelTeacher = model.teachers.find((t) => t.structureIndex === s.index);
      if (!modelTeacher) return;
      const actualTeacher = usersByStructure[s.index].find((u) => u.externalId === modelTeacher.externalId);
      check(actualTeacher, {
        [`sampled teacher of structure ${s.index} was imported`]: (u) => !!u,
      });
      if (!actualTeacher) return;

      const detail = getUserDetail(actualTeacher.id);
      check(detail, {
        [`sampled teacher of structure ${s.index} has the Teacher profile`]:
          (d) => !!d && Array.isArray(d.profiles) && d.profiles[0] === "Teacher",
        [`sampled teacher of structure ${s.index} is linked to at least one class`]:
          (d) => !!d && Array.isArray(d.classes) && d.classes.length > 0,
      });
    });
  });

  group("[Directory][BigAAF] Sampled student/relative linkage", () => {
    model.structures.forEach((s) => {
      const modelStudent = model.students.find((st) => st.structureIndex === s.index);
      if (!modelStudent) return;
      const users = usersByStructure[s.index];
      const actualStudent = users.find((u) => u.externalId === modelStudent.externalId);
      check(actualStudent, {
        [`sampled student of structure ${s.index} was imported`]: (u) => !!u,
      });
      if (!actualStudent) return;

      const studentDetail = getUserDetail(actualStudent.id);
      check(studentDetail, {
        [`sampled student of structure ${s.index} has the expected number of parents`]:
          (d) => !!d && Array.isArray(d.parents) && d.parents.length === modelStudent.relativeLinks.length,
      });

      const firstLink = modelStudent.relativeLinks[0];
      if (!firstLink) return;
      const modelRelative = model.relatives[firstLink.relativeIndex];
      const actualRelative = users.find((u) => u.externalId === modelRelative.externalId);
      check(actualRelative, {
        [`first relative of sampled student of structure ${s.index} was imported`]: (u) => !!u,
      });
      if (!actualRelative) return;

      const childrenRes = http.get(`${rootUrl}/directory/user/${actualRelative.id}/children`, { headers: getHeaders() });
      check(childrenRes, {
        [`relative of sampled student of structure ${s.index} lists them as a child`]: (r) => {
          const body = JSON.parse(<string>r.body);
          return Array.isArray(body) && body.some(
            (entry: any) => Array.isArray(entry.children) &&
              entry.children.some((c: any) => c.externalId === modelStudent.externalId)
          );
        },
      });
    });
  });
}

/*******************************************************************************************************
 *  AAF generation / import helpers
 ******************************************************************************************************/
function buildBigAAFRequest(): AAFFiles {
  const totalStudents = NB_STRUCTURES * NB_CLASSES_PER_STRUCTURE * NB_STUDENTS_PER_CLASS;
  const builder = new AAFGeneratorBuilder()
    .withIndexStart(indexStart)
    .withNbStructures(NB_STRUCTURES)
    .withStructureNameFormat(`${schoolNameBig} {idx}`)
    .withNbClasses(NB_CLASSES_PER_STRUCTURE)
    .withNbStudentsPerClass(NB_STUDENTS_PER_CLASS)
    .withNbTeachersPerStructure(NB_TEACHERS_PER_STRUCTURE)
    .withNbClassesPerTeacher(Math.min(2, NB_CLASSES_PER_STRUCTURE))
    .withNbGroupsPerStructure(NB_GROUPS_PER_STRUCTURE)
    .withNbStudentsPerGroup(NB_STUDENTS_PER_GROUP)
    .withFieldsOfStudy([
      { externalId: "MATH", name: "Mathematiques" },
      { externalId: "FRAN", name: "Francais" },
      { externalId: "HGEO", name: "Histoire-Geographie" },
      { externalId: "ANGL", name: "Anglais" },
    ]);

  STRUCTURE_ATTACHMENTS.forEach(([childIdx, parentIdx]) => builder.withStructureAttachment(childIdx, parentIdx));

  // randomly 1 or 2 parents per student
  for (let i = 0; i < totalStudents; i++) {
    builder.withNbRelativesForStudent(i, Math.random() < 0.5 ? 1 : 2);
  }

  return builder.build().generate();
}

function generateAndImportBigAAF(): AAFGeneratedModel {
  const { files, model } = buildBigAAFRequest();
  importAAFStructureOrFail(files);
  const res = triggerAAFImport();
  assertOk(res, "Big AAF import should be triggered", 202);
  return model;
}

function triggerAAFImport(): RefinedResponse<any> {
  const headers = getHeaders("application/json");
  return http.post(
    `${rootUrl}/directory/api/internal/trigger-import`,
    JSON.stringify({ feeder: "AAF" }),
    { headers },
  );
}

function importAAFStructureOrFail(files: Record<string, string>) {
  const headers = getHeaders("application/json");
  const res = http.post(
    `${rootUrl}/directory/api/internal/upload-aaf`,
    JSON.stringify({ subPath: "test-big", files }),
    { headers },
  );
  assertOk(res, "Big AAF structure files should be uploaded");
}

/** externalIds of every relative referenced by at least one student of the given structure. */
function relativeIndicesOfStructure(model: AAFGeneratedModel, structureIndex: number): Set<number> {
  const indices = new Set<number>();
  model.students
    .filter((st) => st.structureIndex === structureIndex)
    .forEach((st) => st.relativeLinks.forEach((l) => indices.add(l.relativeIndex)));
  return indices;
}

function waitForBigAAFImport(model: AAFGeneratedModel): { structures: Structure[]; usersByStructure: UserInfo[][] } {
  const expectedNames = model.structures.map((s) => `${schoolNameBig} ${s.index}`);
  const endWait = Date.now() + MAX_WAIT_BIG_AAF_IMPORTED * 1000;
  const secondsLeft = () => Math.max(0, Math.floor((endWait - Date.now()) / 1000));

  let structures: Structure[] = [];
  while (Date.now() < endWait) {
    const all = getAllStructures();
    structures = expectedNames
      .map((name) => all.find((s) => s.name === name))
      .filter((s): s is Structure => !!s);
    if (structures.length === model.structures.length) {
      console.log(`All ${structures.length} structures found (${secondsLeft()}s left)`);
      break;
    }
    console.log(`Waiting for structures to be imported (${structures.length}/${model.structures.length})...`);
    sleep(2);
  }
  if (structures.length !== model.structures.length) {
    throw new Error(`Only ${structures.length}/${model.structures.length} structures found after ${MAX_WAIT_BIG_AAF_IMPORTED}s`);
  }
  // re-index by structure.index (order of expectedNames is already aligned with model.structures, but stay explicit)
  const structuresByIndex = model.structures.map((s) => structures.find((st) => st.name === `${schoolNameBig} ${s.index}`)!);

  const expectedUsersByStructure = model.structures.map(
    (s) =>
      model.students.filter((st) => st.structureIndex === s.index).length +
      model.teachers.filter((t) => t.structureIndex === s.index).length +
      relativeIndicesOfStructure(model, s.index).size
  );

  let usersByStructure: UserInfo[][] = structuresByIndex.map(() => []);
  while (Date.now() < endWait) {
    usersByStructure = structuresByIndex.map((s) => getUsersOfSchool(s));
    const ready = usersByStructure.every((users, idx) => users.length >= expectedUsersByStructure[idx]);
    if (ready) {
      console.log(`All users found for every structure (${secondsLeft()}s left)`);
      break;
    }
    console.log(`Waiting for all users to be imported (${usersByStructure.map((u, i) => `${u.length}/${expectedUsersByStructure[i]}`).join(", ")})...`);
    sleep(2);
  }
  usersByStructure.forEach((users, idx) => {
    if (users.length < expectedUsersByStructure[idx]) {
      throw new Error(
        `Structure ${idx} (${structuresByIndex[idx].name}) only has ${users.length}/${expectedUsersByStructure[idx]} users after ${MAX_WAIT_BIG_AAF_IMPORTED}s`
      );
    }
  });

  return { structures: structuresByIndex, usersByStructure };
}

function getUserDetail(userId: string): any {
  const res = http.get(`${rootUrl}/directory/user/${userId}`, { headers: getHeaders() });
  if (res.status !== 200) return null;
  return JSON.parse(<string>res.body);
}
