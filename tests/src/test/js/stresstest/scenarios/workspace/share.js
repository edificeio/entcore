import { check } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";

import {
  authenticateWeb,
  getSchoolByName,
  getUsersOfSchool,
  createStructure,
  createAndSetRole,
  linkRoleToUsers,
  activateUsers,
  uploadFile,
  shareFile,
  createBroadcastGroup
} from "https://raw.githubusercontent.com/juniorode/edifice-k6-commons/develop/dist/index.js";

const rootUrl = __ENV.ROOT_URL;
const maxDuration = __ENV.MAX_DURATION || "1m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General";
const dataRootPath = __ENV.DATA_ROOT_PATH;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const WS_MANAGER_SHARE = [
  "org-entcore-workspace-controllers-WorkspaceController|getDocument",  "org-entcore-workspace-controllers-WorkspaceController|copyDocuments",  "org-entcore-workspace-controllers-WorkspaceController|getDocumentProperties",  "org-entcore-workspace-controllers-WorkspaceController|getRevision",  "org-entcore-workspace-controllers-WorkspaceController|copyFolder",  "org-entcore-workspace-controllers-WorkspaceController|getPreview",  "org-entcore-workspace-controllers-WorkspaceController|copyDocument",  "org-entcore-workspace-controllers-WorkspaceController|getDocumentBase64",  "org-entcore-workspace-controllers-WorkspaceController|listRevisions",  "org-entcore-workspace-controllers-WorkspaceController|commentFolder",  "org-entcore-workspace-controllers-WorkspaceController|commentDocument",  "org-entcore-workspace-controllers-WorkspaceController|shareJson",  "org-entcore-workspace-controllers-WorkspaceController|deleteFolder",  "org-entcore-workspace-controllers-WorkspaceController|restoreFolder",  "org-entcore-workspace-controllers-WorkspaceController|removeShare",  "org-entcore-workspace-controllers-WorkspaceController|moveFolder",  "org-entcore-workspace-controllers-WorkspaceController|moveTrash",  "org-entcore-workspace-controllers-WorkspaceController|restoreTrash",  "org-entcore-workspace-controllers-WorkspaceController|bulkDelete",  "org-entcore-workspace-controllers-WorkspaceController|shareResource",  "org-entcore-workspace-controllers-WorkspaceController|deleteRevision",  "org-entcore-workspace-controllers-WorkspaceController|shareJsonSubmit",  "org-entcore-workspace-controllers-WorkspaceController|moveDocument",  "org-entcore-workspace-controllers-WorkspaceController|renameFolder",  "org-entcore-workspace-controllers-WorkspaceController|moveTrashFolder",  "org-entcore-workspace-controllers-WorkspaceController|deleteComment",  "org-entcore-workspace-controllers-WorkspaceController|getParentInfos",  "org-entcore-workspace-controllers-WorkspaceController|deleteDocument",  "org-entcore-workspace-controllers-WorkspaceController|renameDocument",  "org-entcore-workspace-controllers-WorkspaceController|moveDocuments",  "org-entcore-workspace-controllers-WorkspaceController|updateDocument"
];
const WS_READER_SHARE = ["org-entcore-workspace-controllers-WorkspaceController|getDocument","org-entcore-workspace-controllers-WorkspaceController|copyDocuments","org-entcore-workspace-controllers-WorkspaceController|getDocumentProperties","org-entcore-workspace-controllers-WorkspaceController|getRevision","org-entcore-workspace-controllers-WorkspaceController|copyFolder","org-entcore-workspace-controllers-WorkspaceController|getPreview","org-entcore-workspace-controllers-WorkspaceController|copyDocument","org-entcore-workspace-controllers-WorkspaceController|getDocumentBase64","org-entcore-workspace-controllers-WorkspaceController|listRevisions","org-entcore-workspace-controllers-WorkspaceController|commentFolder","org-entcore-workspace-controllers-WorkspaceController|commentDocument"];
chai.config.logFailures = true;

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
  const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    describe("[Workspace-Init] Initialize data", () => {
      const structure = createStructure(schoolName, {teachers: teachersData, students: studentsData, responsables: responsablesData}, session);
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
    });
  const school = getSchoolByName(schoolName, session);
  const users = getUsersOfSchool(school, session);
  const broadcastGroup = createBroadcastGroup("IT - liste de diffusion profs", school, session);
  return { users , broadcastGroup};
}

export default (data) => {
  describe('[Workspace] Test shares to broadcast groups', () => {
    let res;
    const { users, broadcastGroup } = data;
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
    teacherSession = authenticateWeb(teacher.login, 'password');
    res = shareFile(fileId, shares, teacherSession)
    checkShareOk(res, 'teacher shares to broadcast group')
    // First student tries to share it to thrid student -> ok
    shares.users[thirdStudent.id] = WS_READER_SHARE;
    firstStudentSession = authenticateWeb(firstStudent.login, 'password');
    res = shareFile(fileId, shares, firstStudentSession)
    checkShareOk(res, 'student shares to third student')
    // 1st student tries to remove share to the list -> ko
    delete shares.groups[broadcastGroup.id];
    res = shareFile(fileId, shares, firstStudentSession)
    checkShareKo(res, 'student removes share to broadcast group')
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
  checks[`${checkName} - insufficient rights error returned`] = (r) => JSON.parse(r.body).error === 'insufficient.rights.to.modify.shares'
  checks[`${checkName} - HTTP status`] = (r) => r.status !== 200;
  const ok = check(res, checks)
  if(!ok) {
    console.error(checkName, res)
  }
}

