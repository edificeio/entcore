import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import { check } from "k6";

import {
  authenticateWeb,
  uploadFile,
  downloadFile,
  deleteDocument,
  deleteDocuments,
} from '../../../node_modules/edifice-k6-commons/dist/index.js';
import {
  StorageInitData,
  initStorageFixture,
} from './_utils.ts';

/**
 * Storage.removeFile and Storage.removeFiles — the DeleteObject requests.
 *
 * Both sign a request whose only payload is the key in the URI, so they are the shortest path on which a
 * wrong canonical URI or a wrong signed host surfaces. No existing scenario deletes a document.
 */

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General - One user - Storage delete";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testRemoveFile: {
      executor: "per-vu-iterations",
      exec: "testRemoveFile",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testRemoveFiles: {
      executor: "per-vu-iterations",
      exec: "testRemoveFiles",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  }
};

const dataRootPath = __ENV.DATA_ROOT_PATH || "../../../../resources/data";

let fileToUpload: ArrayBuffer;
try {
  fileToUpload = open(`${dataRootPath}/workspace/small.png`, "b");
} catch (e) {
  fileToUpload = open(`${dataRootPath}/data/workspace/small.png`, "b");
}

export function setup(): StorageInitData {
  return initStorageFixture(schoolName);
}

export function testRemoveFile(data: StorageInitData) {
  describe('[Storage] Remove one file', () => {
    authenticateWeb(data.user.login);

    const uploaded = uploadFile(fileToUpload, "to-delete.png");
    check(downloadFile(uploaded._id), {
      "the file should download before deletion": (r) => r.status === 200,
    });

    const deleteRes = deleteDocument(uploaded._id);
    const ok = check(deleteRes, {
      "delete should be ok": (r) => r.status === 200,
    });
    if (!ok) {
      console.error(`Delete failed: ${deleteRes.status} - ${deleteRes.body}`);
      return;
    }

    // The document row is gone, so the route no longer resolves a file id. A DeleteObject that silently
    // failed leaves the object in the bucket, which this cannot see — what it does pin is that the delete
    // request itself was accepted rather than rejected on its signature.
    check(downloadFile(uploaded._id), {
      "the file should not download after deletion": (r) => r.status === 404 || r.status === 401,
    });
  });
}

export function testRemoveFiles(data: StorageInitData) {
  describe('[Storage] Remove several files', () => {
    authenticateWeb(data.user.login);

    const uploaded = [
      uploadFile(fileToUpload, "batch-1.png"),
      uploadFile(fileToUpload, "batch-2.png"),
      uploadFile(fileToUpload, "batch-3.png"),
    ];
    const ids = uploaded.map((f) => f._id);

    const deleteRes = deleteDocuments(ids);
    const ok = check(deleteRes, {
      "bulk delete should be ok": (r) => r.status === 200,
      "bulk delete should report the three documents": (r) => r.json("number") === 3,
    });
    if (!ok) {
      console.error(`Bulk delete failed: ${deleteRes.status} - ${deleteRes.body}`);
      return;
    }

    for (const id of ids) {
      check(downloadFile(id), {
        "each deleted file should not download": (r) => r.status === 404 || r.status === 401,
      });
    }
  });
}
