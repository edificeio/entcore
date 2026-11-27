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
import { cleanAAFWhenIdle, cleanAllUploadedAAF, importAndWaitOrFail, importedExternalId, uploadAAF } from "./_aaf-utils.ts";


const maxDuration = __ENV.MAX_DURATION || "20m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Directory";
const schoolNameCSV = __ENV.DATA_SCHOOL_NAME_CSV || `${schoolName} CSV`;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const rootUrl = __ENV.ROOT_URL;
const skipInit = __ENV.SKIP_INIT === "true";
const indexStart = parseInt(__ENV.INDEX_START || "9999");
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
    structureCSV = createEmptyStructure(schoolNameCSV, true);
    let usersCSV = getUsersOfSchool(structureCSV);
    if(usersCSV.length < nbDuplicateUsers) {
      for( let i = usersCSV.length; i < nbDuplicateUsers; i++) {
        createUser({
          firstName: `First Name Teach ${i}`,
          lastName: `LAST NAME TEACH ${i}`,
          type: 'Teacher',
          structureId: structureCSV.id,
          birthDate: "01/01/1986",
          positionIds: []});
      }
      usersCSV = getUsersOfSchool(structureCSV);
    }
    initData.csv = { structure: structureCSV, users: usersCSV };


    try {
      structureAAF = generateAAFStructure([{ structureName: schoolName, nbStudents: 10, nbTeachers: 5, nbRelatives: 3 }]);
    } catch (e) {
      // teardown() is not called when setup fails : remove the files now, otherwise every later import of the
      // platform reads them (the AAF1D ones of the feeder scenarios fail with "error.global")
      cleanAAFWhenIdle("AAF", AAF_IMPORT_DIRECTORY);
      throw e;
    }
    users = getUsersOfSchool(structureAAF);
    initData.aaf = { structure: structureAAF, users };


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
  let res = http.get(`${rootUrl}/directory/duplicates?structure=${data.aaf.structure.id}`, { headers: getHeaders() });
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

function generateAAFStructure(parameters: AAFStructureGenerationParameters[]): Structure {
  const { files, model } = generateAAFStructureRequest(parameters);
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
  const users = getUsersOfSchool(structure);
  console.log(`Structure ${structure.name} (${externalId}) imported with ${users.length} users (${expectedNbUsers} expected)`);
  return structure;
}

function generateAAFStructureRequest(parameters: AAFStructureGenerationParameters[]): AAFFiles {
  const builder = new AAFGeneratorBuilder()
    .withIndexStart(indexStart)
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
    // Teacher names must match the "First/Last Name Teach {i}" pattern the CSV-side duplicate
    // users are seeded with in setup() (see nbDuplicateUsers), so cross-structure duplicate
    // detection keeps finding the same overlap it always did.
    for (let i = 0; i < param.nbTeachers; i++) {
      builder
        .withFirstNameForTeacher(teacherOffset + i, `First Name Teach ${i}`)
        .withLastNameForTeacher(teacherOffset + i, `Last Name Teach ${i}`);
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
