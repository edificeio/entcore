import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import { check } from "k6";
import crypto from "k6/crypto";

import {
  authenticateWeb,
  uploadFile,
  downloadFile,
  createFolderOrFail,
  copyDocument,
  copyDocuments,
  copiedIds,
} from '../../../node_modules/edifice-k6-commons/dist/index.js';
import {
  StorageInitData,
  initStorageFixture,
} from './_utils.ts';

/**
 * Storage.copyFile — S3Client.copyFile, the CopyObject request carrying x-amz-copy-source.
 *
 * That header is the one SUPPORT-4854 fixed (it used to be set after the signature, so S3 rejected the
 * request naming it as unsigned) and the one IMPULS-6155 now percent encodes. Nothing exercised it end to
 * end: no k6 scenario copies a document, and the workspace copy route is the only way in.
 */

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General - One user - Storage copy";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testCopyDocument: {
      executor: "per-vu-iterations",
      exec: "testCopyDocument",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testCopyDocumentsInBulk: {
      executor: "per-vu-iterations",
      exec: "testCopyDocumentsInBulk",
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

export function testCopyDocument(data: StorageInitData) {
  describe('[Storage] Copy one document', () => {
    authenticateWeb(data.user.login);

    const original = uploadFile(fileToUpload, "small.png");
    const folderId = createFolderOrFail("Copies " + Date.now());

    const copyRes = copyDocument(original._id, folderId);
    let ok = check(copyRes, {
      "copy should be ok": (r) => r.status === 200,
      "copy should return one document": (r) => copiedIds(r).length === 1,
    });
    if (!ok) {
      console.error(`Copy failed: ${copyRes.status} - ${copyRes.body}`);
      return;
    }
    const copyId = copiedIds(copyRes)[0];

    check(copyId, {
      "the copy should be a distinct document": (id) => id !== original._id,
    });

    // The point of the scenario: the copy is readable, and holds the very same bytes. A CopyObject that
    // silently failed, or copied the wrong key, shows up here and nowhere else.
    const originalBytes = downloadFile(original._id, "", "binary");
    const copyBytes = downloadFile(copyId, "", "binary");
    ok = check({ originalBytes, copyBytes }, {
      "original should still download": (r) => r.originalBytes.status === 200,
      "copy should download": (r) => r.copyBytes.status === 200,
      "copy should have the same size": (r) =>
          (r.copyBytes.body as ArrayBuffer).byteLength === (r.originalBytes.body as ArrayBuffer).byteLength,
      "copy should be byte for byte identical": (r) =>
          crypto.sha256(r.copyBytes.body as ArrayBuffer, "hex") ===
          crypto.sha256(r.originalBytes.body as ArrayBuffer, "hex"),
    });
    if (!ok) {
      console.error(`Original status ${originalBytes.status}, copy status ${copyBytes.status}`);
    }
  });
}

export function testCopyDocumentsInBulk(data: StorageInitData) {
  describe('[Storage] Copy several documents', () => {
    authenticateWeb(data.user.login);

    const first = uploadFile(fileToUpload, "first.png");
    const second = uploadFile(fileToUpload, "second.png");
    const folderId = createFolderOrFail("Bulk copies " + Date.now());

    const copyRes = copyDocuments([first._id, second._id], folderId);
    const ok = check(copyRes, {
      "bulk copy should be ok": (r) => r.status === 200,
      "bulk copy should return two documents": (r) => copiedIds(r).length === 2,
    });
    if (!ok) {
      console.error(`Bulk copy failed: ${copyRes.status} - ${copyRes.body}`);
      return;
    }

    // Each copy triggers its own CopyObject: one signature per file, so a header set after the signature
    // fails on all of them rather than on the first only.
    for (const copyId of copiedIds(copyRes)) {
      const res = downloadFile(copyId, "", "binary");
      check(res, {
        "each copy should download": (r) => r.status === 200,
        "each copy should have content": (r) => (r.body as ArrayBuffer).byteLength > 0,
      });
    }
  });
}
