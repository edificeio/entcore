import { check, sleep } from "k6";
import {chai, describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  getTeacherRole,
  authenticateWeb,
  getStudentRole,
  getParentRole,
  getUsersOfSchool,
  createStructure,
  createAndSetRole,
  linkRoleToUsers,
  activateUsers,
  
  triggerImport,
} from "https://raw.githubusercontent.com/juniorode/edifice-k6-commons/develop/dist/index.js";

const aafImport = (__ENV.AAF_IMPORT || "true") === "true";
const aafImportPause =  parseInt(__ENV.AAF_IMPORT_PAUSE || "10");
const maxDuration = __ENV.MAX_DURATION || "1m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "MaxBG";
const dataRootPath = __ENV.DATA_ROOT_PATH;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
chai.config.logFailures = true;
const broadcastGroupName = "IT - MaxBG profs"

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    shareFile: {
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "30s",
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

const teachersData = open(`${dataRootPath}/enseignants.csv`, "b");

export function setup() {
  let structure1;
  describe("[Auth-Init] Initialize data", () => {
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure1 = initStructure(schoolName, session)
    const teacherGroupRole = getTeacherRole(structure1, session)
    /*for(let i = 0; i < 7000; i++) {
      const commonBG = createBroadcastGroup(`BG de la structure ${i}`, structure1, session);
      addCommRuleToGroup(commonBG.id, [teacherGroupRole.id], session)
    }*/
    if(aafImport) {
      triggerImport(session)
      sleep(aafImportPause)
    }
  });
  return { structure1};
}

function initStructure(structureName, session) {
  const structure = createStructure(structureName, {teachers: teachersData}, session);
  const role = createAndSetRole('Espace documentaire', session);
  const groups = [
    `Teachers from group ${structure.name}.`,
    `Enseignants du groupe ${structure.name}.`,
    `Students from group ${structure.name}.`,
    `Élèves du groupe ${structure.name}.`,
    `Relatives from group ${structure.name}.`,
    `Parents du groupe ${structure.name}.`
  ]
  linkRoleToUsers(structure, role, groups, session);
  activateUsers(structure, session);
  return structure
}

export default (data) => {
  const { structure1 } = data;
  describe('[Auth] Test authenticate user with many groups', () => {
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users1 = getUsersOfSchool(structure1, session);
    const teacher1 = getRandomUserWithProfile(users1, 'Teacher');
    authenticateWeb(teacher1.login, 'password');
  })
};

function getRandomUser(arrayOfUsers, exceptUsers) {
  const idToAvoid = (exceptUsers || []).map(u => u.id)
  for(let i = 0; i < 1000; i++) {
    const user = arrayOfUsers[Math.floor(Math.random() * arrayOfUsers.length)]
    if(idToAvoid.indexOf(user.id) < 0) {
      return user;
    }
  }
  throw 'cannot.find.random.user'
}

function getRandomUserWithProfile(arrayOfUsers, profileGroup, exceptUsers) {
  const usersOfGroup = arrayOfUsers.filter(u => u.type === profileGroup)
  return getRandomUser(usersOfGroup, exceptUsers)
}
