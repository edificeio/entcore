import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import { check } from "k6";

import {
  authenticateWeb,
  Session,
  Structure,
  createEmptyStructure,
} from '../../../node_modules/edifice-k6-commons/dist/index.js';
import { postMassMessagingColumnMapping } from './_utils.ts';

/**
 * Storage.copyDirectoryToFs — the whole-prefix download: a ListObjectsV2 over the prefix, then one GetObject
 * per key, each written to the local file system.
 *
 * DefaultMassMessagingService.csvColumnsMapping calls it directly, and reports its failure as a distinct
 * error ("Failed to copy import files from storage to FS"), so this route tells apart a storage problem from
 * a parsing one. The other caller, CsvValidator, reaches it behind the feeder and gives no such signal.
 *
 * The route is admin only (AdminFilter) and MFA protected, so the scenario runs as the ADMC. On an
 * environment where MFA is enforced it will answer 401 rather than 200 — that is a configuration mismatch,
 * not a storage regression.
 */

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General - One user - Storage csv";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testCopyDirectoryToFs: {
      executor: "per-vu-iterations",
      exec: "testCopyDirectoryToFs",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  }
};

const dataRootPath = __ENV.DATA_ROOT_PATH || "../../../../resources/data";

let teachersCsv: ArrayBuffer;
try {
  teachersCsv = open(`${dataRootPath}/positions/csv/before/enseignants.csv`, "b");
} catch (e) {
  teachersCsv = open(`${dataRootPath}/data/positions/csv/before/enseignants.csv`, "b");
}

type InitData = {
  head: Structure;
}

export function setup(): InitData {
  let structure: Structure | null = null;
  describe("[Storage-Init] Initialize data", () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = createEmptyStructure(`${schoolName}`, true);
  });
  return { head: structure };
}

export function testCopyDirectoryToFs(data: InitData) {
  describe('[Storage] Pull an uploaded directory back from storage', () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const res = postMassMessagingColumnMapping(data.head.id, data.head.name, teachersCsv);
    const ok = check(res, {
      "the column mapping should be ok": (r) => r.status === 200,
      // The discriminating check: this is the message the service returns when copyDirectoryToFs fails,
      // whatever the reason — a rejected signature, a listing that came back empty, a GetObject that 404ed.
      "the files should have been copied back from storage": (r) =>
          r.body != null && (r.body as string).indexOf("Failed to copy import files from storage to FS") < 0,
      "no error should be reported at all": (r) => {
        const error = r.json("error");
        return error === undefined || error === null;
      },
    });
    if (!ok) {
      console.error(`Column mapping failed: ${res.status} - ${res.body}`);
      return;
    }

    // And the CSV really was read from the copied directory: the mapping holds one record per line.
    check(res, {
      "the mapping should hold the CSV records": (r) => {
        const records = r.json("asmRecords");
        return Array.isArray(records) && records.length > 0;
      },
      "each record should hold columns": (r) => {
        const records = r.json("asmRecords") as any[];
        return Array.isArray(records) && records.every((row) => Array.isArray(row) && row.length > 0);
      },
    });
  });
}
