import { sleep } from "k6";
import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  getAdmlsOrMakThem,
  triggerImport,
  initStructure,
  searchVisibles,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import { checkSearchVisible } from "./_utils.ts";

const aafImport = (__ENV.AAF_IMPORT || "true") === "true";
const aafImportPause =  parseInt(__ENV.AAF_IMPORT_PAUSE || "10");
const maxDuration = __ENV.MAX_DURATION || "1m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Search visibles - Tests";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    searchVisiblesLight: {
      exec: 'searchVisiblesLight',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

export function setup() {
  let structure;
  describe("[Search visibles light] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initStructure(schoolName)
  });
  if(aafImport) {
    triggerImport()
    sleep(aafImportPause)
  }
  return {structure}
}

/* For this test to complete successfully, recette must run with the following configuration in communication config section of ent-core.json :
 - "visibles-search-type": "light"
 */
export function searchVisiblesLight(data) {
  const {structure} = data;
  describe('[Communication] Test - Search visibles light', () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const admlTeacher = getAdmlsOrMakThem(structure, 'Teacher', 1, [])[0]
    authenticateWeb(admlTeacher.login)
    const res = searchVisibles();
    const expectedUserFields = ["id", "displayName", "profile", "children", "relatives", "type", "usedIn"];
    const expectedGroupFields = ["id", "displayName", "profile", "nbUsers", "type", "groupType", "usedIn"];
    checkSearchVisible(res, expectedUserFields, expectedGroupFields, 'Search visibles light')
  });
}
