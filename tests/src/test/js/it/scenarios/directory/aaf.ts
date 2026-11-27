import { RefinedResponse } from "k6/http";
import {
  authenticateWeb,
  Structure,
  getUsersOfSchool,
  getHeaders,
  UserProfileType,
  UserInfo,
  getAllStructures,
  createEmptyStructure,
  assertOk,
  createUser
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import http from "k6/http";
import { check, group } from "k6";
import { AAFFiles, AAFGeneratorBuilder } from "./_aaf-generator.ts";
import { cleanAAFWhenIdle, cleanAllUploadedAAF, distinctById, importAndWaitOrFail, importedExternalId, newRunSeed, uploadAAF } from "./_aaf-utils.ts";


const maxDuration = __ENV.MAX_DURATION || "20m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Directory";
const schoolNameCSV = __ENV.DATA_SCHOOL_NAME_CSV || `${schoolName} CSV`;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const rootUrl = __ENV.ROOT_URL;
const skipInit = __ENV.SKIP_INIT === "true";
/** First id of the generated data : a new one at every run (see newRunSeed), INDEX_START replays one. */
const INDEX_START = __ENV.INDEX_START;
const nbDuplicateUsers = parseInt(__ENV.NB_DUPLICATE_USERS || "3");
/** Import directory of the files uploaded by this test. Removed at the end : see teardown(). */
const AAF_IMPORT_DIRECTORY = "test";

const types: UserProfileType[] = ['Teacher', 'Relative', 'Student'];

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testImportAAF: {
      executor: "per-vu-iterations",
      exec: "testImportAAF",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  aaf: {
    structure: Structure;
    users: UserInfo[];
  };
  csv: {
    structure: Structure;
    users: UserInfo[];
  };
}

type AAFStructureGenerationParameters = {
  structureName: string;
  nbStudents: number;
  nbTeachers: number;
  nbRelatives: number;
}

export function setup() {
  const initData: InitData = {
  }
  let structureAAF: Structure;
  let structureCSV: Structure;
  let users: UserInfo[];

  group("[Directory] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    // teardown() does not run when a previous run (of this test or of another one) was killed or failed in setup :
    // start from clean import directories
    cleanAllUploadedAAF();
    const seed = INDEX_START ? parseInt(INDEX_START) : newRunSeed();
    structureCSV = createEmptyStructure(schoolNameCSV, true);
    // the teachers of this run, created by hand : the AAF teachers of the same names are their duplicates.
    // The structure can be an existing one (without FORCE_RECREATE_SCHOOLS) : only the missing ones are created
    let usersCSV = distinctById(getUsersOfSchool(structureCSV));
    for (let i = 0; i < nbDuplicateUsers; i++) {
      const name = duplicateTeacherName(seed, i);
      if (usersCSV.some((u) => u.firstName === name.firstName && u.lastName === name.lastName.toUpperCase())) continue;
      createUser({
        firstName: name.firstName,
        lastName: name.lastName.toUpperCase(),
        type: 'Teacher',
        structureId: structureCSV.id,
        birthDate: "01/01/1986",
        positionIds: []});
    }
    usersCSV = distinctById(getUsersOfSchool(structureCSV));
    const missing = Array.from({ length: nbDuplicateUsers }, (_, i) => duplicateTeacherName(seed, i))
      .filter((name) => !usersCSV.some((u) => u.firstName === name.firstName && u.lastName === name.lastName.toUpperCase()));
    if (missing.length > 0) {
      throw new Error(`The teachers ${missing.map((n) => `${n.firstName} ${n.lastName}`).join(", ")} could not be created ` +
        `in the CSV structure ${structureCSV.name} : the duplicates checked by this test cannot all exist`);
    }
    initData.csv = { structure: structureCSV, users: usersCSV };


    try {
      structureAAF = generateAAFStructure(seed, [{ structureName: schoolName, nbStudents: 10, nbTeachers: 5, nbRelatives: 3 }]);
    } catch (e) {
      // teardown() is not called when setup fails : remove the files now, otherwise every later import of the
      // platform reads them (the AAF1D ones of the feeder scenarios fail with "error.global")
      cleanAAFWhenIdle("AAF", AAF_IMPORT_DIRECTORY);
      throw e;
    }
    users = distinctById(getUsersOfSchool(structureAAF));
    initData.aaf = { structure: structureAAF, users };
    // read again : the duplicates of the CSV teachers are marked by the AAF import
    initData.csv.users = distinctById(getUsersOfSchool(structureCSV));


  });
  console.log("[Directory] Setup completed");
  return initData;
}

/*******************************************************************************************************
 *  User Endpoints
 ******************************************************************************************************/
export function testImportAAF(data: InitData) {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  const idsOfDuplicatedUsersInCSV = data.csv.users.filter(u => u.duplicates && u.duplicates.length > 0).map((u) => u.id);
  const idsOfDuplicatedUsersInAAF = data.aaf.users.filter(u => u.duplicates && u.duplicates.length > 0).map((u) => u.id);
  // with a structure filter, only the pairs whose 2 users are in the given structures are listed : both structures
  // are needed to get the CSV / AAF pairs (one per CSV teacher, the AAF structure is new at every run)
  let res = http.get(`${rootUrl}/directory/duplicates?structure=${data.aaf.structure.id}&structure=${data.csv.structure.id}`, { headers: getHeaders() });
  check(res, {
    'list duplicates with structure filter returns 200': (r) => r.status === 200,
    'list duplicates with structure filter is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    'list duplicates with structure filter has expected length': (r) => JSON.parse(<string>r.body).length === nbDuplicateUsers,
    'list duplicates with structure filter has expected content': (r) => {
      const duplicates = JSON.parse(<string>r.body);
      return duplicates.every((d: any) => {
        const userId1 = d.user1.id;
        const userId2 = d.user2.id;
        return idsOfDuplicatedUsersInCSV.includes(userId1) && idsOfDuplicatedUsersInAAF.includes(userId2) ||
               idsOfDuplicatedUsersInCSV.includes(userId2) && idsOfDuplicatedUsersInAAF.includes(userId1);
      });
    }
  });

  res = http.get(`${rootUrl}/directory/duplicates`, { headers: getHeaders() });
  const duplicates = new Set();
  JSON.parse(<string>res.body).forEach((d: any) => {
    const userId1 = d.user1.id;
    const userId2 = d.user2.id;
    duplicates.add(userId1);
    duplicates.add(userId2);
  });
  check(res, {
    'list duplicates without structure filter returns 200': (r) => r.status === 200,
    'list duplicates without structure filter is array': (r) => Array.isArray(JSON.parse(<string>r.body)),
    'list duplicates without structure filter has expected length': (r) => JSON.parse(<string>r.body).length >= nbDuplicateUsers,
    'list duplicates without structure filter has all duplicated of CSV structure': (r) => {
      return idsOfDuplicatedUsersInCSV.every((id) => duplicates.has(id));
    },
    'list duplicates without structure filter has all duplicated of AAF structure': (r) => {
      return idsOfDuplicatedUsersInAAF.every((id) => duplicates.has(id));
    }
  });
}

/**
 * Leaves no file behind : a directory which stays listed in importDirectories.json is read by every later import of
 * the platform, including the AAF1D ones, which fail on it.
 */
export function teardown() {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  const failure = cleanAAFWhenIdle("AAF", AAF_IMPORT_DIRECTORY);
  check(failure, {
    [`the AAF files uploaded in "${AAF_IMPORT_DIRECTORY}" have been removed${failure ? ` (${failure})` : ""}`]: (f) => f === null,
  });
}

function generateAAFStructure(seed: number, parameters: AAFStructureGenerationParameters[]): Structure {
  const { files, model } = generateAAFStructureRequest(seed, parameters);
  importAAFStructureOrFail(files);
  // waits for the post import too : the duplicates checked by this test are marked there
  importAndWaitOrFail("AAF");
  // the structure is looked up by externalId : "Directory" is a common name, older runs left structures named so
  const externalId = importedExternalId(AAF_IMPORT_DIRECTORY, model.structures[0].externalId);
  const structure = getAllStructures().find((s) => s.externalId === externalId);
  if (!structure) {
    throw new Error(`Structure ${externalId} not found after the import : was it attached to an existing structure with the same UAI (${model.structures[0].uai}) ?`);
  }
  const expectedNbUsers = parameters[0].nbStudents + parameters[0].nbTeachers + parameters[0].nbStudents * parameters[0].nbRelatives;
  const users = distinctById(getUsersOfSchool(structure));
  console.log(`Structure ${structure.name} (${externalId}) imported with ${users.length} users (${expectedNbUsers} expected)`);
  return structure;
}

/**
 * Name of the i-th teacher of the run, shared by the CSV and the AAF structures so that they are duplicates (the CSV
 * one gets an upper-cased last name). The seed is in both names : the teachers of the older runs match neither of them,
 * so they are not duplicates of the ones of this run (the feeder needs 2 equal fields among last name, first name,
 * birth date and email). It also makes the logins new at every run : the logins of
 * the users created by hand are generated from the names, by several instances of the feeder which each keep their
 * own cache of the logins in use, and may give a login already taken by another instance (the creation then fails).
 */
function duplicateTeacherName(seed: number, i: number): { firstName: string; lastName: string } {
  return { firstName: `First Name Teach ${seed} ${i}`, lastName: `Last Name Teach ${seed} ${i}` };
}

function generateAAFStructureRequest(seed: number, parameters: AAFStructureGenerationParameters[]): AAFFiles {
  const builder = new AAFGeneratorBuilder()
    .withIndexStart(seed)
    .withNbStructures(parameters.length);

  let studentOffset = 0;
  let teacherOffset = 0;
  parameters.forEach((param, index) => {
    // one class per structure: every student/teacher of that structure lands in it, like the
    // previous hand-rolled template did with its single fixed "1TES 2" class.
    builder
      .withStructureNameForStructure(index, param.structureName)
      .withNbClassesForStructure(index, 1)
      .withNbStudentsForClass(index, param.nbStudents)
      .withNbTeachersForStructure(index, param.nbTeachers);
    for (let s = 0; s < param.nbStudents; s++) {
      builder.withNbRelativesForStudent(studentOffset + s, param.nbRelatives);
    }
    // the first nbDuplicateUsers teachers are the duplicates of the CSV teachers created in setup()
    for (let i = 0; i < param.nbTeachers; i++) {
      const name = duplicateTeacherName(seed, i);
      builder
        .withFirstNameForTeacher(teacherOffset + i, name.firstName)
        .withLastNameForTeacher(teacherOffset + i, name.lastName);
    }
    studentOffset += param.nbStudents;
    teacherOffset += param.nbTeachers;
  });

  return builder.build().generate();
}

export function importAAFStructure(files: Record<string, string>): RefinedResponse<any> {
  return uploadAAF("AAF", AAF_IMPORT_DIRECTORY, files);
}

export function importAAFStructureOrFail(files: Record<string, string>) {
  const res = importAAFStructure(files);
  assertOk(res, "AAF structure should be imported");
}
