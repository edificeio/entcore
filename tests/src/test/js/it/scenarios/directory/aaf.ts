import { RefinedResponse } from "k6/http";
import {
  authenticateWeb,
  Structure,
  getUsersOfSchool,
  getHeaders,
  UserProfileType,
  UserInfo,
  getSchoolByName,
  createEmptyStructure,
  assertOk,
  createUser
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import http from "k6/http";
import {check, group, sleep} from "k6";
import { AAFGeneratorBuilder } from "./_aaf-generator.ts";


const maxDuration = __ENV.MAX_DURATION || "20m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Directory";
const schoolNameCSV = __ENV.DATA_SCHOOL_NAME_CSV || `${schoolName} CSV`;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const rootUrl = __ENV.ROOT_URL;
const skipInit = __ENV.SKIP_INIT === "true";
const indexStart = parseInt(__ENV.INDEX_START || "9999");
const MAX_WAIT_STRUCTURE_IMPORTED = parseInt(__ENV.MAX_WAIT_STRUCTURE_IMPORTED || "30");
const nbDuplicateUsers = parseInt(__ENV.NB_DUPLICATE_USERS || "3");

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


    structureAAF = generateAAFStructure([{ structureName: schoolName, nbStudents: 10, nbTeachers: 5, nbRelatives: 3 }]);
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

function generateAAFStructure(parameters: AAFStructureGenerationParameters[]): Structure {
  const request = generateAAFStructureRequest(parameters);
  importAAFStructureOrFail(request);
  const res = triggerAAFImport();
  assertOk(res, "AAF import should be triggered", 202);
  let structure: Structure | null = null;
  let endWait = Date.now() + MAX_WAIT_STRUCTURE_IMPORTED * 1000;
  while(Date.now() < endWait) {
    structure = getSchoolByName(schoolName);
    if(structure) {
      console.log(`Structure ${schoolName} found after ${MAX_WAIT_STRUCTURE_IMPORTED - Math.floor((endWait - Date.now()) / 1000)} seconds`);
      break;
    }
    console.log(`Waiting for structure ${schoolName} to be imported...`);
    sleep(1);
  }
  if(!structure) {
    throw new Error(`Structure ${schoolName} not found after ${MAX_WAIT_STRUCTURE_IMPORTED} seconds`);
  }
  while(Date.now() < endWait) {
    const expectedNbUsers = parameters[0].nbStudents + parameters[0].nbTeachers + parameters[0].nbStudents * parameters[0].nbRelatives;
    const users = getUsersOfSchool(structure);
    if(users.length >= expectedNbUsers) {
      console.log(`All users found after ${MAX_WAIT_STRUCTURE_IMPORTED - Math.floor((endWait - Date.now()) / 1000)} seconds`);
      break;
    }
    console.log(`Waiting for all users to be imported (${users.length} out of ${expectedNbUsers})...`);
    sleep(1);
  }
  return structure;
}

function triggerAAFImport(): RefinedResponse<any> {
  const headers = getHeaders("application/json");
  const triggerRequest = {
    feeder: "AAF"
  };
  const res = http.post(
    `${rootUrl}/directory/api/internal/trigger-import`,
    JSON.stringify(triggerRequest),
    { headers },
  );
  return res;
}

function generateAAFStructureRequest(parameters: AAFStructureGenerationParameters[]): Record<string, string> {
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

  return builder.build().generate().files;
}

export function importAAFStructure(files: Record<string, string>): RefinedResponse<any> {
  const request: AAFFilesUploadRequest = {
    subPath: "test",
    files: files,
  };
  const headers = getHeaders("application/json");
  const res = http.post(
    `${rootUrl}/directory/api/internal/upload-aaf`,
    JSON.stringify(request),
    { headers },
  );
  return res;
}

export function importAAFStructureOrFail(files: Record<string, string>) {
  const res = importAAFStructure(files);
  assertOk(res, "AAF structure should be imported");
}

export type AAFFilesUploadRequest = {
  subPath: string;
  files: Record<string, string>;
}