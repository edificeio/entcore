import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  getUsersOfSchool,
  initStructure,
  getRandomUserWithProfile,
  Session,
  Structure,
  createAndSetRole,
  linkRoleToUsers,
  uploadFile,
  downloadFile
} from '../../../node_modules/edifice-k6-commons/dist/index.js';
import { check, sleep } from "k6";

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testUploadFile: {
      executor: "per-vu-iterations",
      exec: "testUploadFile",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  head: Structure;
}

export function setup() {
  let structure: Structure | null = null;
  describe("[Workspace-Init] Initialize data", () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initStructure(`${schoolName}`, 'tiny')
    const role = createAndSetRole('Espace documentaire');
    const groups = [
        `Teachers from group ${structure.name}.`,
        `Enseignants du groupe ${structure.name}.`,
        `Students from group ${structure.name}.`,
        `Élèves du groupe ${structure.name}.`,
        `Relatives from group ${structure.name}.`,
        `Parents du groupe ${structure.name}.`
    ]
    linkRoleToUsers(structure, role, groups);
  });
  return { head: structure };
}

const dataRootPath = __ENV.DATA_ROOT_PATH;
let fileToUpload;
try {
  fileToUpload = open(`${dataRootPath}/workspace/big-picture.jpg`, "b");
} catch(e) {
  fileToUpload = open(`${dataRootPath}/data/workspace/big-picture.jpg`, "b");
}

export function testUploadFile(data: InitData) {

  describe('[Workspace] Upload file', () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const headUsers = getUsersOfSchool(data.head);
    const teacher1 = getRandomUserWithProfile(headUsers, 'Teacher');

    console.log("Authenticate teacher1 " + teacher1.login);
    authenticateWeb(teacher1.login);

    console.log("Uploading big file, it may take a while...");
    const uploadedFile = uploadFile(fileToUpload, "big-picture.jpg");
    console.log(`File uploaded with id ${uploadedFile._id} and size ${uploadedFile.size} bytes`);
    const fileId = uploadedFile._id;
    console.log("Waiting for image resizer to do its job...");
    sleep(5);
    console.log("Downloading thumbnail...");
    const thumbnailFile = downloadFile(fileId, '120x120');
    console.log(`Thumbnail downloaded`);
    const ok = check(thumbnailFile, {
      "should download thumbnail": (r) => r.status === 200,
      "should have content": (r) => r.body.length > 0,
      "should have correct content-type": (r) => r.headers['Content-Type'] === 'image/jpeg',
      "should have correct thumbnail size": (r) => parseInt(r.headers['Content-Length']) <= 5_000, // Should be around 4776 bytes for a 120x120 thumbnail of a jpeg image
    });
    if (!ok) {
      console.error(`Status is ${thumbnailFile.status}`);
      console.error(`Headers are ${JSON.stringify(thumbnailFile.headers)}`);
    }
  });
}
