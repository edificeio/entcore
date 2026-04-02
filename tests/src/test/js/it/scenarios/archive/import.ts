import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  Session,
  Structure,
  createAndSetRole,
  linkRoleToUsers,
  createUserAndGetData,
  createEmptyStructure,
  activateUsers,
  AppImportResult,
  importArchive,
  analyzeImport,
  launchImport,
  ImportAnalysisResult,
  UserInfo
} from '../../../node_modules/edifice-k6-commons/dist/index.js';
import { check } from "k6";

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General - One user - Archive";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testImport: {
      executor: "per-vu-iterations",
      exec: "testImport",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  }
};

type InitData = {
  head: Structure;
  user: UserInfo;
}

const dataRootPath = __ENV.DATA_ROOT_PATH;

let zipFileToUpload: ArrayBuffer;
try {
  zipFileToUpload = open(`${dataRootPath}/archive/import.zip`, "b");
} catch(e) {
  zipFileToUpload = open(`${dataRootPath}/data/archive/import.zip`, "b");
}
export function setup() {
  let structure: Structure | null = null;
  let user: UserInfo | null = null;
  describe("[Workspace-Init] Initialize data", () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = createEmptyStructure(`${schoolName}`, true)
    user = createUserAndGetData({
      firstName: "Importer " + Date.now(),
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
      createAndSetRole('Blog'),
      createAndSetRole('Actualites'),
      createAndSetRole('Cahier Multimédia'),
    ];
    const groups = [
        `Teachers from group ${structure.name}.`,
    ]
    for (const role of roles) {
      linkRoleToUsers(structure, role, groups);
    }
  });
  return { head: structure, user };
}

export function testImport(data: InitData) {

  describe('[Archive] Import archive', () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const teacher1 = data.user;

    console.log("Authenticate teacher1 " + teacher1.login);
    authenticateWeb(teacher1.login);

    console.log("Upload archive file");
    const importRes = importArchive(zipFileToUpload);
    let ok = check(importRes, {
      "Import should be ok": (res) => res.status === 200,
      "Import upload should return an id": (res) => res.json("importId") !== undefined,
    });
    const importId = importRes.json("importId");
    console.log(`Import analysis id: ${importId}`);
    const analysisResult = analyzeImport(importId);
    check(analysisResult, {
      "Import analysis should be ok": (res) => res.status === 200,
      "Import analysis should have results for all apps": (r) => {
        const res = r.json() as ImportAnalysisResult;
        const expectedApps = ['scrapbook', 'blog', 'actualites', 'workspace'];
        return expectedApps.every(app => app in res.apps);
      },
    });
    const apps = analysisResult.json("apps") as ImportAnalysisResult;
    const launchRes = launchImport(importId, { apps });

    check(launchRes, {
      "Import launch should be ok": (res) => res.status === 200,
      "Import result should have results for all apps": (r) => {
        const res = r.json() as Record<string, AppImportResult>;
        const expectedApps = ['scrapbook', 'blog', 'actualites', 'workspace'];
        return expectedApps.every(app => app in res);
      },
      "Import result should have success status for all apps": (r) => {
        const res = r.json() as Record<string, AppImportResult>;
        return Object.values(res).every((appResult: AppImportResult) => appResult.status !== "error");
      }
    });
  })
}
