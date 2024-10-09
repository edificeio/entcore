import { check, sleep } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";

import {
  getTeacherRole,
  authenticateWeb,
  getStudentRole,
  getParentRole,
  getUsersOfSchool,
  createAndSetRole,
  linkRoleToUsers,
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
  createEmptyStructure,
  initStructure,
  getRandomUser,
  getRandomUserWithProfile
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";

const aafImport = (__ENV.AAF_IMPORT || "true") === "true";
const aafImportPause =  parseInt(__ENV.AAF_IMPORT_PAUSE || "10");
const maxDuration = __ENV.MAX_DURATION || "1m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General";
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

const fileToUpload = open(`${dataRootPath}/workspace/random_text_file.txt`, "b");

export function setup() {
  let structure1;
  let structure2;
  let chapeau;
  describe("[Workspace-Init] Initialize data", () => {
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    chapeau = createEmptyStructure(`Chapeau - ${schoolName}`, false, session)
    const commonBG = createBroadcastGroup(broadcastGroupNameChapeau, chapeau, session)
    structure1 = initSchool(schoolName, session)
    structure2 = initSchool(schoolName2, session)
    attachStructureAsChild(chapeau, structure1, session)
    attachStructureAsChild(chapeau, structure2, session)
    const teacherRole1 = getTeacherRole(structure1, session)
    const teacherRole2 = getTeacherRole(structure2, session)
    const parentRole2 = getParentRole(structure2, session)
    const studentRole2 = getStudentRole(structure2, session)
    const groupIds = [
      teacherRole1.id,
      teacherRole2.id
    ]
    addCommRuleToGroup(commonBG.id, groupIds, session)
    addCommRuleToGroup(parentRole2.id, [teacherRole1.id], session)
    addCommRuleToGroup(studentRole2.id, [parentRole2.id], session)
    if(aafImport) {
      triggerImport(session)
      sleep(aafImportPause)
    }
  });
  return { structure1, structure2, chapeau};
}

function initSchool(structureName, session) {
  const structure = initStructure(structureName);
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
  const teacherRole = getTeacherRole(structure, session)
  const broadcastGroup = createBroadcastGroup(broadcastGroupName, structure, session);
  addCommunicationBetweenGroups(teacherRole.id, broadcastGroup.id, session)
  return structure
}

export default (data) => {
  testSharesViaBroadcastGroupInSameSchool(data)
  testSharesViaBroadcastGroupInDifferentSchools(data)
  testSharesViaProfileGroupInDifferentSchools(data)
};


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

function testSharesViaProfileGroupInDifferentSchools(data) {
  const { structure1, structure2 } = data;
  describe('[Workspace] Test shares via profile groups in two different schools', () => {
    let res;
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users1 = getUsersOfSchool(structure1, session);
    const users2 = getUsersOfSchool(structure2, session);
    const parentRole2 = getParentRole(structure2, session)
    const studentRole2 = getStudentRole(structure2, session)
    const teacher1 = getRandomUserWithProfile(users1, 'Teacher');
    const teacher12 = getRandomUserWithProfile(users1, 'Teacher', [teacher1]);
    const parent = getRandomUserWithProfile(users2, 'Relative')
    console.log("Teacher 1 - ", teacher1.login);
    console.log("Teacher 1.2 - ", teacher12.login);
    console.log("Parent - ", parent.login);
    // Teacher upload a file
    let teacherSession = authenticateWeb(teacher1.login, 'password');
    const uploadedFile = uploadFile(fileToUpload, teacherSession);
    const fileId = uploadedFile._id;
    // Share this file to parents 2 as a manager -> ok
    const shares = {bookmarks: {}, groups: {}, users: {}}
    shares.groups[parentRole2.id] = WS_MANAGER_SHARE;
    res = shareFile(fileId, shares, teacherSession);
    checkShareOk(res, 'teacher of school 1 shares to parents group of school 2')
    // Parent of school 2 tries to share it to students of school 2 -> ok
    let parentSession = authenticateWeb(parent.login, 'password');
    shares.groups[studentRole2.id] = WS_READER_SHARE;
    res = shareFile(fileId, shares, parentSession)
    checkShareOk(res, 'parent of school 2 shares to students of school 2')
    // Teacher 2 of school 1 tries to modify shares of students of school 2 -> ko
    shares.groups[studentRole2.id] = WS_MANAGER_SHARE;
    let teacher12Session = authenticateWeb(teacher12.login, 'password')
    res = shareFile(fileId, shares, teacher12Session)
    checkShareKo(res, 'other teacher of school 1 tries to modify shares of student of school 2')
    // Parent of school 2 tries to do the same thing -> ok
    switchSession(parentSession);
    res = shareFile(fileId, shares, teacher12Session)
    checkShareOk(res, 'parent of school 2 tries to modify shares of student of school 2')
    // Parent of school 2 tries to add teacher of school 1 (i.e. the creator) as a manager -> ok
    shares.users[teacher1.id] = WS_MANAGER_SHARE
    res = shareFile(fileId, shares, teacher12Session)
    checkShareOk(res, 'parent of school 2 tries to add creator as a manager')
  })
}
function testSharesViaBroadcastGroupInDifferentSchools(data) {
  const { structure1, structure2, chapeau } = data;
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
  })
}

function testSharesViaBroadcastGroupInSameSchool(data) {
  const { structure1 } = data;
  describe('[Workspace] Test shares to broadcast group in the same school', () => {
    let res;
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(structure1, session);
    const broadcastGroup = getBroadcastGroup(broadcastGroupName, structure1, session);
    const students = users.filter(u => u.type === 'Student')
    const teacher = getRandomUserWithProfile(users, 'Teacher');
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
    // First student tries to share it to third student -> ok
    shares.users[thirdStudent.id] = WS_READER_SHARE;
    switchSession(firstStudentSession)
    res = shareFile(fileId, shares, firstStudentSession)
    checkShareOk(res, 'student shares to third student')
    // thrid student tries to remove share to the list -> ko
    delete shares.groups[broadcastGroup.id];
    let thirdStudentSession = authenticateWeb(thirdStudent.login, 'password')
    res = shareFile(fileId, shares, thirdStudentSession)
    checkShareKo(res, 'non manager cannot remove shares')
  })
}