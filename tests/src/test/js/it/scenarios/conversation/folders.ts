/**
 * K6 test script for testing conversation folder functionalities.
 *
 * This script performs the following actions:
 * - Authenticates a user.
 * - Initializes a school structure and assigns roles to users.
 * - Creates folders and subfolders.
 * - Creates a draft message and moves it into a subfolder.
 * - Deletes folders and verifies that the draft message is trashed.
 *
 * Environment Variables:
 * - `MAX_DURATION`: Maximum duration for the test scenario.
 * - `DATA_SCHOOL_NAME`: Name of the school for the test data.
 * - `DATA_ROOT_PATH`: Root path for the test data.
 * - `GRACEFUL_STOP`: Graceful stop duration for the test scenario.
 * - `ADMC_LOGIN`: Admin login for authentication.
 * - `ADMC_PASSWORD`: Admin password for authentication.
 *
 * Functions:
 * - `setup()`: Initializes the test data and returns the school structure.
 * - `initSchool()`: Initializes the school structure and assigns roles to users.
 * - `checkCreateDraftOk(res, checkName)`: Checks if the draft message creation was successful.
 * - `checkDeleteFolderOk(res, draftId, checkName)`: Checks if the folder deletion was successful and the draft message is trashed.
 * - `testDeleteFoldersAndTrashMessages(data)`: Main test function that performs the folder and message operations.
 *
 * Exported Default Function:
 * - `(data)`: Executes the `testDeleteFoldersAndTrashMessages` function with the provided data.
 */
import { check } from "k6";
import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  getUsersOfSchool,
  createAndSetRole,
  linkRoleToUsers,
  triggerImport,
  initStructure,
  getRandomUserWithProfile,
  DraftMessage,
  createDraftMessage,
  getMessage,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import {
  checkCreateFolderOk,
  checkMoveIntoFolderOk,
  createFolder,
  deleteFolder,
  moveIntoFolder,
} from "./_folders.ts";

const maxDuration = __ENV.MAX_DURATION || "1m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Conversation - Tests";
const dataRootPath = __ENV.DATA_ROOT_PATH;
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    conversationFoldersTest: {
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

export function setup() {
  let structure;
  describe("[Conversation-Init] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initSchool();
  });
  return { structure };
}

function initSchool() {
  const structure = initStructure(schoolName);
  const role = createAndSetRole("Messagerie");
  const groups = [
    `Teachers from group ${structure.name}.`,
    `Enseignants du groupe ${structure.name}.`,
  ];
  linkRoleToUsers(structure, role, groups);
  return structure;
}

function checkCreateDraftOk(res, checkName) {
  const checks = {};
  checks[`${checkName} - HTTP status`] = (r) => r.status === 201;
  const ok = check(res, checks);
  if (!ok) {
    console.error(checkName, res);
  }
}

function checkDeleteFolderOk(res, draftId, checkName) {
  const checks = {};
  checks[`${checkName} - HTTP status`] = (r) => r.status === 200;
  checks[`${checkName} - Draft in subfolder is trashed`] = (r) => {
    const body = JSON.parse(r.body);
    return (
      body.trashedMessageIds instanceof Array &&
      body.trashedMessageIds.length === 1 &&
      body.trashedMessageIds[0] === draftId
    );
  };
  const ok = check(res, checks);
  if (!ok) {
    console.error(checkName, res);
  }
}

function testDeleteFoldersAndTrashMessages(data) {
  const { structure } = data;
  describe("[Conversation] Test - Create draft and send message", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(structure);
    const teacher1 = getRandomUserWithProfile(users, "Teacher");
    authenticateWeb(teacher1.login);
    const draftMessage = <DraftMessage>{
      to: [],
      cc: [],
      cci: [],
      subject: "message from teacher 1",
      body: "<div>No real body</div>",
    };

    // Teacher creates a first folder
    let res = createFolder({ name: "Folder 1" });
    checkCreateFolderOk(res, "Teacher creates a first folder");
    const folder1Id = JSON.parse(res.body as string).id;
    // Teacher creates a subfolder
    res = createFolder({ name: "Folder 1.1", parentId: folder1Id });
    checkCreateFolderOk(res, "Teacher creates a subfolder");
    const folder11Id = JSON.parse(res.body as string).id;
    // Teacher creates a second folder
    res = createFolder({ name: "Folder 2" });
    checkCreateFolderOk(res, "Teacher creates a second folder");
    const folder2Id = JSON.parse(res.body as string).id;

    // Teacher creates draft message
    res = createDraftMessage(draftMessage);
    checkCreateDraftOk(res, "Teacher creates draft message");
    const draftId = JSON.parse(res.body as string).id;

    // Teacher puts draft message into subfolder
    res = moveIntoFolder(folder11Id, [draftId]);
    checkMoveIntoFolderOk(res, "Teacher puts message into subfolder");

    // Teacher deletes the first folder with subfolders recursively.
    res = deleteFolder(folder1Id);
    checkDeleteFolderOk(res, draftId, "Teacher deletes the first folders");
  });
}

export default (data) => {
  testDeleteFoldersAndTrashMessages(data);
};
