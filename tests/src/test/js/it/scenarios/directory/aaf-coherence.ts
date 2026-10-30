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
} from "edifice-k6-commons";
import http from "k6/http";
import {check, group, sleep} from "k6";
import { AAFGeneratorBuilder } from "./_aaf-generator.js";


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
    testImportBigAAF: {
      executor: "per-vu-iterations",
      exec: "testImportAAFCoherence",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};


/*******************************************************************************************************
 *  User Endpoints
 ******************************************************************************************************/
export function testImportAAFCoherence() {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  
}