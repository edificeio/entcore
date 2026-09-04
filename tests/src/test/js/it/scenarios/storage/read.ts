import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import { check } from "k6";
import crypto from "k6/crypto";
import encoding from "k6/encoding";

import {
  authenticateWeb,
  uploadFile,
  downloadFile,
  getDocumentBase64,
} from '../../../node_modules/edifice-k6-commons/dist/index.js';
import {
  StorageInitData,
  initStorageFixture,
} from './_utils.ts';

/**
 * Storage.readFile — the GetObject whose whole body is buffered in memory, as opposed to Storage.sendFile
 * which streams it to the response. The two take different paths through S3Client, and only sendFile is
 * covered today (by workspace/nominal.ts, through the download and thumbnail routes).
 *
 * The fixture is deliberately a text file, not an image: with the image resizer wired to the bucket, an
 * uploaded image is re-encoded on the way in — small.png goes from 201 to 94 bytes — so "what came back
 * equals what was sent" would be asserting the optimiser away rather than the read. A text file travels
 * untouched, which is what makes the byte comparison below mean something.
 *
 * Storage.fileStats is deliberately not here: on a workspace document the properties route reads Mongo, not
 * the bucket. The only route that reaches fileStats is /archive/export/verify/:exportId, already exercised
 * by workspace/export.ts.
 */

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General - One user - Storage read";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testReadFile: {
      executor: "per-vu-iterations",
      exec: "testReadFile",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  }
};

const dataRootPath = __ENV.DATA_ROOT_PATH || "../../../../resources/data";

let fileToUpload: ArrayBuffer;
try {
  fileToUpload = open(`${dataRootPath}/workspace/random_text_file.txt`, "b");
} catch (e) {
  fileToUpload = open(`${dataRootPath}/data/workspace/random_text_file.txt`, "b");
}

export function setup(): StorageInitData {
  return initStorageFixture(schoolName);
}

export function testReadFile(data: StorageInitData) {
  describe('[Storage] Read a file into memory', () => {
    authenticateWeb(data.user.login);

    const uploaded = uploadFile(fileToUpload, "to-read.txt");

    const base64Res = getDocumentBase64(uploaded._id);
    let ok = check(base64Res, {
      "base64 read should be ok": (r) => r.status === 200,
      "base64 read should return content": (r) => {
        const encoded = r.json("base64File");
        return typeof encoded === "string" && encoded.length > 0;
      },
      "base64 read should return the document name": (r) => r.json("title") === "to-read.txt",
    });
    if (!ok) {
      console.error(`Base64 read failed: ${base64Res.status} - ${base64Res.body}`);
      return;
    }

    // What readFile buffered has to be the file that was uploaded, not a truncated or re-encoded version of
    // it: a wrong payload hash on the upload signature would have been rejected, but a partial read would
    // not, and only a byte comparison catches that.
    const decoded = encoding.b64decode(base64Res.json("base64File") as string, "std", "b");
    check(decoded, {
      "the buffered content should have the uploaded size": (d) =>
          (d as ArrayBuffer).byteLength === fileToUpload.byteLength,
      "the buffered content should be the uploaded bytes": (d) =>
          crypto.sha256(d as ArrayBuffer, "hex") === crypto.sha256(fileToUpload, "hex"),
    });

    // And the streaming path returns the same thing, so readFile and sendFile cannot drift apart.
    const streamed = downloadFile(uploaded._id, "", "binary");
    check(streamed, {
      "the streamed content should match the buffered one": (r) =>
          r.status === 200 &&
          crypto.sha256(r.body as ArrayBuffer, "hex") === crypto.sha256(decoded as ArrayBuffer, "hex"),
    });
  });
}
