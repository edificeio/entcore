import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  getUsersOfSchool,
  getRandomUserWithProfile,
  Session,
  Structure,
  createAndSetRole,
  linkRoleToUsers,
  uploadZip,
  uploadFile,
  downloadExportFile,
  createUserAndGetData,
  createEmptyStructure,
  activateUsers,
  launchExportOrFail,
  verifyExportFiles,
} from '../../../node_modules/edifice-k6-commons/dist/index.js';
import { check, fail, sleep } from "k6";

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General - One user - Archive";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testExport: {
      executor: "per-vu-iterations",
      exec: "testExport",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  }
};

type InitData = {
  head: Structure;
}

const dataRootPath = __ENV.DATA_ROOT_PATH;
const EXPORT_TIMEOUT = 30 * 60 * 1000; // 30 minutes

let fileToUpload: ArrayBuffer;
let zipFileToUpload: ArrayBuffer;
try {
  fileToUpload = open(`${dataRootPath}/workspace/small.png`, "b");
  zipFileToUpload = open(`${dataRootPath}/workspace/test.zip`, "b");
} catch(e) {
  fileToUpload = open(`${dataRootPath}/data/workspace/small.png`, "b");
  zipFileToUpload = open(`${dataRootPath}/data/workspace/test.zip`, "b");
}
export function setup() {
  let structure: Structure | null = null;
  describe("[Workspace-Init] Initialize data", () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = createEmptyStructure(`${schoolName}`, true)
    const user = createUserAndGetData({
      firstName: "Uploader",
      lastName: "User",
      "type": "Teacher",
      structureId: structure.id,
      birthDate: "2020-01-01",
      positionIds: []
    })
    activateUsers(structure);
    const roles = [
      createAndSetRole('Espace documentaire'),
      createAndSetRole('Archive'),
    ];
    const groups = [
        `Teachers from group ${structure.name}.`,
        `Enseignants du groupe ${structure.name}.`,
        `Students from group ${structure.name}.`,
        `Élèves du groupe ${structure.name}.`,
        `Relatives from group ${structure.name}.`,
        `Parents du groupe ${structure.name}.`
    ]
    for (const role of roles) {
      linkRoleToUsers(structure, role, groups);
    }
  });
  return { head: structure };
}

export function testExport(data: InitData) {

  describe('[Workspace] Upload file', () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const headUsers = getUsersOfSchool(data.head);
    const teacher1 = getRandomUserWithProfile(headUsers, 'Teacher');

    console.log("Authenticate teacher1 " + teacher1.login);
    authenticateWeb(teacher1.login);

    console.log("Uploading small file, it may take a while...");
    uploadFile(fileToUpload, "small.png");
    console.log("... and a zip file");
    uploadZip(zipFileToUpload, "test.zip");
    console.log("Files uploaded, launching export...");
    let exportId = launchExportOrFail(["workspace"]);
    console.log(`Export launched with id ${exportId}, waiting for it to be ready...`);
    // Call verifyExportFiles in a loop until the export is ready (i.e. status 200) or until a timeout is reached
    const startTime = Date.now();
    let exportReady = false;
    while (!exportReady && (Date.now() - startTime) < EXPORT_TIMEOUT) { // 30 minutes timeout
      const verifyRes = verifyExportFiles(exportId);
      if (verifyRes.status === 200) {
        exportReady = true;
      } else if(verifyRes.status === 500) {
        fail(`Export failed with status 500. Response: ${verifyRes.body}`);
      } else if(verifyRes.status === 404) {
        fail(`Export not found with status 404. Response: ${verifyRes.body}`);
      } else {
        console.log(`Export not ready yet (status: ${verifyRes.status}; body: ${verifyRes.body}). Retrying ...`);
        sleep(1);
      }
    }
    check(exportReady, {
      "export should be ready within timeout": (ready) => ready,
    });
    if (!exportReady) {
      return;
    }
    console.log("Export is ready, downloading file...");
    const downloadRes = downloadExportFile(exportId);
    check(downloadRes, {
      "should download export file successfully": (r) => r.status === 200,
    });
    // Check that the zip contains the uploaded files - this is a basic check to ensure the export is correct, we don't check the whole content of the zip for performance reasons
    check(String(downloadRes.body), {
      "export file should contain small.png": (content) => content.indexOf("small.png") >= 0,
      "export file should contain 002.png": (content) => content.indexOf("002.png") >= 0,
      "export file should contain 001.png": (content) => content.indexOf("001.png") >= 0,
    });
  })
}
