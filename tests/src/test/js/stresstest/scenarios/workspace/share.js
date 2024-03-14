import { check, sleep } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";

import {
  authenticateWeb,
  getTeacherRole,
  getUsersOfSchool,
  createStructure,
  createAndSetRole,
  linkRoleToUsers,
  activateUsers,
  uploadFile,
  shareFile,
  getBroadcastGroup,
  createBroadcastGroup,
  addCommRuleToGroup,
  addCommunicationBetweenGroups,
  attachStructureAsChild,
  switchSession,
  WS_READER_SHARE,
  WS_MANAGER_SHARE,
  triggerImport,
  createEmptyStructure
} from "https://raw.githubusercontent.com/juniorode/edifice-k6-commons/develop/dist/index.js";

const aafImport = (__ENV.AAF_IMPORT || "true") === "true";
const aafImportPause =  parseInt(__ENV.AAF_IMPORT_PAUSE || "10");
const maxDuration = __ENV.MAX_DURATION || "1m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General2";
const schoolName2 = `${schoolName}-2`
const dataRootPath = __ENV.DATA_ROOT_PATH;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
chai.config.logFailures = true;
const broadcastGroupName = "IT - liste de diffusion profs"
const broadcastGroupNameChapeau = "IT - Chapeau liste de diffusion profs"

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

const teachersData = open(`${dataRootPath}/general/enseignants.csv`, "b");
const studentsData = open(`${dataRootPath}/general/eleves.csv`, "b");
const responsablesData = open(`${dataRootPath}/general/responsables.csv`, "b");
const fileToUpload = open(`${dataRootPath}/random_text_file.txt`, "b");

export function setup() {
  let structure1;
  let structure2;
  let chapeau;
  describe("[Workspace-Init] Initialize data", () => {
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    chapeau = createEmptyStructure(`Chapeau - ${schoolName}`, false, session)
    const commonBG = createBroadcastGroup(broadcastGroupNameChapeau, chapeau, session)
    structure1 = initStructure(schoolName, session)
    structure2 = initStructure(schoolName2, session)
    attachStructureAsChild(chapeau, structure1, session)
    attachStructureAsChild(chapeau, structure2, session)
    const groupIds = [
      getTeacherRole(structure1, session).id,
      getTeacherRole(structure2, session).id
    ]
    addCommRuleToGroup(commonBG.id, groupIds, session)
    if(aafImport) {
      triggerImport(session)
      sleep(aafImportPause)
    }
  });
  return { structure1, structure2, chapeau};
}

function initStructure(structureName, session) {
  const structure = createStructure(structureName, {teachers: teachersData, students: studentsData, responsables: responsablesData}, session);
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
  const teacherRole = getTeacherRole(structure, session)
  const broadcastGroup = createBroadcastGroup(broadcastGroupName, structure, session);
  addCommunicationBetweenGroups(teacherRole.id, broadcastGroup.id, session)
  return structure
}

// Tests :
// - au sein de la mm école
// - entre plusieurs écoles
// - entre plusieurs écoles mais le gestionnaire initial est supprimé => vérifier que le 2ème gestionnaire peut supprimer des gens
//   vers qui il n'a pas de visibilité
// - différencier auteur et gestionnaire

export default (data) => {
  const { structure1, structure2, chapeau } = data;
  describe('[Workspace] Test shares to broadcast group in the same school', () => {
    let res;
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(structure1, session);
    const broadcastGroup = getBroadcastGroup(broadcastGroupName, structure1, session);
    const teachers = users.filter(u => u.type === 'Teacher')
    const students = users.filter(u => u.type === 'Student')
    const teacher = getRandomUser(teachers);
    const classId = teacher.classes[0].id
    const classUsers = students.filter(u => u.classes.map(c => c.id).indexOf(classId) >= 0);
    const firstStudent = getRandomUser(classUsers);
    const secondStudent = getRandomUser(classUsers, [firstStudent]);
    const thirdStudent = getRandomUser(classUsers, [firstStudent, secondStudent]);
    console.log("Teacher   - ", teacher.login);
    console.log("Student 1 - ", firstStudent.login);
    console.log("Student 2 - ", secondStudent.login);
    console.log("Student 3 - ", thirdStudent.login);
    // Teacher upload a file
    let teacherSession = authenticateWeb(teacher.login, 'password');
    const uploadedFile = uploadFile(fileToUpload, teacherSession);
    const fileId = uploadedFile._id;
    // Share this file to firstStudent as a manager
    const shares = {bookmarks: {}, groups: {}, users: {}}
    shares.users[firstStudent.id] = WS_MANAGER_SHARE;
    res = shareFile(fileId, shares, teacherSession);
    checkShareOk(res, 'teacher shares to first student')
    // First student tries to share it to the list -> ko
    let firstStudentSession = authenticateWeb(firstStudent.login, 'password');
    shares.groups[broadcastGroup.id] = WS_READER_SHARE;
    res = shareFile(fileId, shares, firstStudentSession)
    checkShareKo(res, 'student shares to broadcast group')
    // First student tries to share it to secondStudent -> ok
    delete shares.groups[broadcastGroup.id];
    shares.users[secondStudent.id] = WS_READER_SHARE;
    res = shareFile(fileId, shares, firstStudentSession)
    checkShareOk(res, 'student shares to second student')
    // Teacher shares it with the list -> ok
    shares.groups[broadcastGroup.id] = WS_READER_SHARE;
    switchSession(teacherSession)
    res = shareFile(fileId, shares, teacherSession)
    checkShareOk(res, 'teacher shares to broadcast group')
    // First student tries to share it to thrid student -> ok
    shares.users[thirdStudent.id] = WS_READER_SHARE;
    switchSession(firstStudentSession)
    res = shareFile(fileId, shares, firstStudentSession)
    checkShareOk(res, 'student shares to third student')
    // 1st student tries to remove share to the list -> ko
    delete shares.groups[broadcastGroup.id];
    res = shareFile(fileId, shares, firstStudentSession)
    checkShareKo(res, 'student removes share to broadcast group')
  })
  describe('[Workspace] Test shares to broadcast group in two different schools', () => {
    let res;
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users1 = getUsersOfSchool(structure1, session);
    const users2 = getUsersOfSchool(structure2, session);
    const broadcastGroup = getBroadcastGroup(broadcastGroupNameChapeau, chapeau, session);
    const broadcastGroupSchool1 = getBroadcastGroup(broadcastGroupName, structure1, session);
    const teachers1 = users1.filter(u => u.type === 'Teacher')
    const teachers2 = users2.filter(u => u.type === 'Teacher')
    const students1 = users1.filter(u => u.type === 'Student')
    const students2 = users2.filter(u => u.type === 'Student')
    const teacher1 = getRandomUser(teachers1);
    const teacher2 = getRandomUser(teachers2);
    const classId1 = teacher1.classes[0].id
    const classId2 = teacher2.classes[0].id
    const classUsers1 = students1.filter(u => u.classes.map(c => c.id).indexOf(classId1) >= 0);
    const classUsers2 = students2.filter(u => u.classes.map(c => c.id).indexOf(classId2) >= 0);
    const firstStudent1 = getRandomUser(classUsers1);
    const firstStudent2 = getRandomUser(classUsers2);
    console.log("Teacher 1 - ", teacher1.login);
    console.log("Teacher 2 - ", teacher2.login);
    console.log("Student 1 - ", firstStudent1.login);
    console.log("Student 2 - ", firstStudent2.login);
    // Teacher upload a file
    let teacherSession = authenticateWeb(teacher1.login, 'password');
    const uploadedFile = uploadFile(fileToUpload, teacherSession);
    const fileId = uploadedFile._id;
    // Share this file to teacher 2 as a manager
    const shares = {bookmarks: {}, groups: {}, users: {}}
    shares.groups[broadcastGroup.id] = WS_MANAGER_SHARE;
    res = shareFile(fileId, shares, teacherSession);
    checkShareOk(res, 'teacher of school 1 shares to chapeau broadcast group')
    // Teacher of school 2 shares it to one of the student of school 2 => ok
    let teacher2Session = authenticateWeb(teacher2.login, 'password');
    shares.users[firstStudent2.id] = WS_MANAGER_SHARE;
    res = shareFile(fileId, shares, teacher2Session);
    checkShareOk(res, 'teacher of school 2 shares to student of school 2')
    // Student of school 2 tries to share it to student of school 1 -> ko
    let studentSession = authenticateWeb(firstStudent1.login, 'password');
    shares.users[firstStudent1.id] = WS_READER_SHARE;
    res = shareFile(fileId, shares, studentSession)
    checkShareKo(res, 'student of school 2 shares to student of school 1')
    delete shares.users[firstStudent1.id]
    // Student of school 2 tries to share it to broadcast group of school 1 -> ko
    shares.groups[broadcastGroupSchool1.id] = WS_MANAGER_SHARE;
    res = shareFile(fileId, shares, studentSession)
    checkShareKo(res, 'student of school 2 shares to student of school 1')
    delete shares.groups[broadcastGroupSchool1.id]
    // Teacher of school 1 tries to remove the student of school 2 -> ko
    delete shares.users[firstStudent2.id];
    switchSession(teacherSession)
    res = shareFile(fileId, shares, teacherSession)
    checkShareKo(res, 'teacher of school 1 tries to remove share of student of school 2')
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

function checkShareOk(res, checkName) {
  const checks = {}
  checks[checkName] = (r) => r.status === 200
  const ok = check(res, checks);
  if(!ok) {
    console.error(checkName, res)
  }
}

function checkShareKo(res, checkName) {
  const checks = {}
  checks[`${checkName} - HTTP status`] = (r) => r.status !== 200;
  checks[`${checkName} - insufficient rights error returned`] = (r) => {
    try {
      return JSON.parse(r.body).error === 'insufficient.rights.to.modify.shares'
    } catch (e) {
      return true;
    }
  }
  const ok = check(res, checks)
  if(!ok) {
    console.error(checkName, res)
  }
}
