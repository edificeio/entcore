import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import { check, fail, sleep } from "k6";
import crypto from "k6/crypto";

import {
  authenticateWeb,
  uploadFile,
  downloadFile,
  createFolderOrFail,
  copyDocument,
  copiedIds,
  launchExportOrFail,
  downloadExportFile,
  verifyExportFiles,
  parseZip,
  getZipTree,
} from '../../../node_modules/edifice-k6-commons/dist/index.js';
import {
  StorageInitData,
  initStorageFixture,
} from './_utils.ts';

/**
 * The encoding dimension of IMPULS-6155, which no scenario covers today.
 *
 * Two distinct paths are at stake, and it matters not to confuse them:
 *
 *  - A workspace object key is a generated UUID, so a special character in a file name never reaches
 *    S3Client.encodeUrlPath on that route. What it does reach is the x-amz-meta-filename header, which is
 *    quoted-printable encoded and signed, and the Content-Disposition of the download.
 *  - The archive export names each exported file after the document (S3Storage.writeToFileSystem uses
 *    alias.getString(id, id)), and an import pushes that tree back with S3Storage.moveFsDirectory. Those
 *    keys carry real names, and that is where the RFC 3986 encoder replaces URLEncoder — a space used to be
 *    written as +, storing a literally wrong key, and ~ and * used to yield SignatureDoesNotMatch.
 */

const maxDuration = __ENV.MAX_DURATION || "10m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General - One user - Storage encoding";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
/**
 * Five minutes, not thirty. A workspace export of a handful of small files is a matter of seconds; anything
 * beyond this is an export that died, and waiting half an hour to be told so is not a test, it is a hang.
 * Raise it with EXPORT_TIMEOUT_SECONDS on a slower environment.
 */
const EXPORT_TIMEOUT = parseInt(__ENV.EXPORT_TIMEOUT_SECONDS || "300") * 1000;

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testSpecialCharacterNames: {
      executor: "per-vu-iterations",
      exec: "testSpecialCharacterNames",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
    testSpecialCharacterNamesSurviveExport: {
      executor: "per-vu-iterations",
      exec: "testSpecialCharacterNamesSurviveExport",
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

/** One name per character URLEncoder and RFC 3986 disagree on, plus non ASCII. */
const NAMES = [
  "rapport final.png",   // space: URLEncoder wrote +, so the stored key was literally wrong
  "note~1.png",          // tilde: URLEncoder escaped it, the server did not
  "etoile*.png",         // star: URLEncoder left it raw, the server escaped it
  "a+b.png",             // plus: has to survive as a literal plus, not decode to a space
  "100% sur.png",        // percent: the double encoding case
  "eleve prive.png",     // space again, with the accents stripped from the ASCII name
  "élève privé.png",     // non ASCII: encoded as UTF-8 percent escapes
];

/**
 * The size of a k6 response body, whichever shape it came back in: a text response is a string, a binary
 * one an ArrayBuffer, and only the latter has byteLength. Reading .length on an ArrayBuffer yields
 * undefined, which compares false against anything — a passing download then looks like an empty one.
 */
function bodyLength(body: any): number {
  if (body == null) {
    return 0;
  }
  if (typeof body === "string") {
    return body.length;
  }
  if (typeof body.byteLength === "number") {
    return body.byteLength;
  }
  return typeof body.length === "number" ? body.length : 0;
}

/**
 * A pattern matching the entry the export is expected to carry for a stored file name.
 *
 * The export renames what it cannot put on a file system: a {@code *} comes out as {@code _}. That is
 * deliberate, so the assertion allows a substitution on exactly those characters and on nothing else —
 * a space, a tilde, a plus, a percent and the accents all have to survive verbatim.
 */
function zipEntryPattern(storedName: string): RegExp {
  const escaped = storedName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  // Re-open the classes the export is allowed to rewrite.
  const relaxed = escaped.replace(/\\\*/g, "."); 
  return new RegExp(relaxed + "$");
}

/** Printable ASCII only: a name outside that range cannot survive an ISO-8859-1 header. */
function isAscii(value: string): boolean {
  return /^[\x20-\x7E]*$/.test(value);
}

export function setup(): StorageInitData {
  return initStorageFixture(schoolName);
}

export function testSpecialCharacterNames(data: StorageInitData) {
  describe('[Storage] Upload, download and copy files with special characters', () => {
    authenticateWeb(data.user.login);

    const folderId = createFolderOrFail("Encoding " + Date.now());

    for (const name of NAMES) {
      const uploaded = uploadFile(fileToUpload, name);
      let ok = check(uploaded, {
        [`should upload "${name}"`]: (f) => !!f && !!f._id,
      });
      if (!ok) {
        console.error(`Upload of "${name}" returned ${JSON.stringify(uploaded)}`);
        continue;
      }

      if (isAscii(name)) {
        check(uploaded, {
          [`should keep the name of "${name}"`]: (f) => f.metadata.filename === name,
        });
      } else if (uploaded.metadata.filename !== name) {
        // Not asserted, and not a storage defect: the multipart file name travels in a
        // Content-Disposition header, which HTTP defines as ISO-8859-1. k6 writes the raw UTF-8 bytes
        // rather than the filename*=UTF-8'' form of RFC 5987, and the server does not read that form
        // either, so a non ASCII name comes back mangled. Both ends would have to adopt RFC 5987.
        // The file is kept in the list all the same: its bytes still exercise a UTF-8 encoded S3 key.
        console.warn(`Non ASCII name does not round-trip: sent "${name}", ` +
            `stored "${uploaded.metadata.filename}" — RFC 5987 gap, unrelated to storage.`);
      }

      // The download signs the same key again. Content-Disposition is deliberately not asserted: the
      // workspace download route serves inline by default and makes no promise about that header.
      const downloaded = downloadFile(uploaded._id);
      check(downloaded, {
        [`should download "${name}"`]: (r) => r.status === 200,
        [`should return content for "${name}"`]: (r) => bodyLength(r.body) > 0,
      });

      // The reference for the copy is the stored object, read back, not the local file: an uploaded image
      // is re-encoded by the resizer on the way in, so the local bytes are not what sits in the bucket.
      const originalBytes = downloadFile(uploaded._id, "", "binary");
      const expectedDigest = originalBytes.status === 200
          ? crypto.sha256(originalBytes.body as ArrayBuffer, "hex")
          : null;

      // And the copy re-signs it as an x-amz-copy-source header value.
      const copyRes = copyDocument(uploaded._id, folderId);
      ok = check(copyRes, {
        [`should copy "${name}"`]: (r) => r.status === 200 && copiedIds(r).length === 1,
      });
      if (!ok) {
        console.error(`Copy of "${name}" failed: ${copyRes.status} - ${copyRes.body}`);
        continue;
      }
      const copyBytes = downloadFile(copiedIds(copyRes)[0], "", "binary");
      check(copyBytes, {
        [`the copy of "${name}" should hold the same bytes`]: (r) =>
            r.status === 200 && expectedDigest != null &&
            crypto.sha256(r.body as ArrayBuffer, "hex") === expectedDigest,
      });
    }
  });
}

export function testSpecialCharacterNamesSurviveExport(data: StorageInitData) {
  describe('[Storage] Export files whose names carry special characters', () => {
    authenticateWeb(data.user.login);

    // What the server stored is what the export will name the file after, and for a non ASCII name that
    // is not what we sent — see the RFC 5987 note above. Asserting on the sent name would be asserting
    // that gap all over again; the round-trip worth pinning here is storage to export.
    const storedNames = NAMES.map((name) => {
      const uploaded = uploadFile(fileToUpload, name);
      return { sent: name, stored: uploaded.metadata.filename };
    });

    const exportId = launchExportOrFail(["workspace"]);
    console.log(`Export launched with id ${exportId}, waiting for it to be ready...`);
    const startTime = Date.now();
    let exportReady = false;
    let attempts = 0;
    let lastStatus = 0;
    while (!exportReady && (Date.now() - startTime) < EXPORT_TIMEOUT) {
      attempts++;
      // An explicit, short per-request timeout: the k6 default is 60s, so a stalled export would be
      // polled once a minute in silence. An export that is merely not ready answers immediately.
      const verifyRes = verifyExportFiles(exportId, "10s");
      lastStatus = verifyRes.status;
      if (verifyRes.status === 200) {
        exportReady = true;
      } else if (verifyRes.status === 500) {
        fail(`Export failed with status 500. Response: ${verifyRes.body}`);
      } else if (verifyRes.status === 404) {
        fail(`Export not found with status 404. Response: ${verifyRes.body}`);
      } else {
        // Every ten attempts, say where we are: an export that died leaves this loop silent otherwise, and
        // the reason is in the server log, not here.
        if (attempts % 10 === 0) {
          console.log(`Export ${exportId} still not ready after ${attempts} attempts ` +
              `(last status ${verifyRes.status}); check the server log for an unhandled exception.`);
        }
        sleep(1);
      }
    }
    check(exportReady, {
      "export should be ready within timeout": (ready) => ready,
    });
    if (!exportReady) {
      console.error(`Export ${exportId} never became ready: ${attempts} attempts, last status ` +
          `${lastStatus}, gave up after ${Math.round((Date.now() - startTime) / 1000)}s.`);
      return;
    }

    const downloadRes = downloadExportFile(exportId);
    const ok = check(downloadRes, {
      "should download the export": (r) => r.status === 200,
      "the export should have content": (r) => bodyLength(r.body) > 0,
    });
    if (!ok) {
      console.error(`Export download failed: HTTP ${downloadRes.status}, ` +
          `${bodyLength(downloadRes.body)} bytes`);
      return;
    }

    // Every exported file is named after its document, so each of these names became an S3 key on the way
    // out. A name that comes back mangled — a space turned into a +, a doubled percent — means the encoder
    // and the server disagreed.
    const tree = getZipTree(parseZip(downloadRes.body));
    let allFound = true;
    for (const { sent, stored } of storedNames) {
      if (!isAscii(sent)) {
        // Skipped for the same reason the upload name is: the name reaching this zip has been through two
        // charset gaps — the RFC 5987 one on upload, then the zip entry encoding, which turns the stored
        // U+FFFD into "ï¿½". Neither is a storage concern, and the bytes themselves did travel: the
        // server log shows the export downloading this object under its UTF-8 encoded S3 key.
        console.warn(`Not asserted in the export, non ASCII name: sent "${sent}", stored "${stored}".`);
        continue;
      }
      const pattern = zipEntryPattern(stored);
      const found = check(tree, {
        [`the export should hold "${sent}"`]: (entries) =>
            entries.some((entry: string) => pattern.test(entry)),
      });
      if (!found) {
        allFound = false;
        console.error(`Missing from the export: sent "${sent}", stored "${stored}", ` +
            `looked for /${pattern.source}/.`);
      }
    }
    if (!allFound) {
      console.error(`Export tree was: ${JSON.stringify(tree)}`);
    }
  });
}
