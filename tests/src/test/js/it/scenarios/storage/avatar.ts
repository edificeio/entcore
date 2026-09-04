import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import { check, sleep } from "k6";
import crypto from "k6/crypto";

import {
  authenticateWeb,
  getConnectedUserId,
  uploadFile,
} from '../../../node_modules/edifice-k6-commons/dist/index.js';
import {
  StorageInitData,
  initStorageFixture,
  updateAvatar,
  getAvatar,
} from './_utils.ts';

/**
 * Storage.findByFilenameEndingWith, and the Storage.removeFiles that follows it.
 *
 * On S3Storage this is the only caller of S3Client.getObjectsEndingWith, which issues a ListObjectsV2 —
 * "list-type=2&prefix=..." — so it is the one route that signs a request carrying several query parameters.
 * That is exactly what IMPULS-6155 touches: the values used to go through URLEncoder rather than an RFC 3986
 * encoder, and the parameter ordering was left to the caller.
 *
 * The trigger is an avatar update: DefaultUserBookService.update calls cleanAvatarCache whenever the payload
 * carries a "picture", which lists the cached thumbnails of that user and removes them.
 *
 * How the listing gets pinned despite cleanAvatarCache swallowing its failures: the second update has to
 * drop the avatars cached by the first one before caching its own. If the listing came back empty when it
 * should not have, nothing would be removed and the route would keep serving the first picture — so the
 * check that the served bytes changed between the two updates is what actually proves the listing found
 * its keys. The route answering 200 alone would prove nothing.
 *
 * Caveat worth knowing before reading a failure: Directory builds the avatar storage as an S3Storage only
 * when the directory config holds an "s3avatars" block, and falls back to FileStorage otherwise. Without
 * that block this scenario still passes, but it exercises FileStorage — it says nothing about SigV4.
 */

const maxDuration = __ENV.MAX_DURATION || "5m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "General - One user - Storage avatar";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testFindByFilenameEndingWith: {
      executor: "per-vu-iterations",
      exec: "testFindByFilenameEndingWith",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  }
};

const dataRootPath = __ENV.DATA_ROOT_PATH || "../../../../resources/data";

let firstPicture: ArrayBuffer;
let secondPicture: ArrayBuffer;
try {
  firstPicture = open(`${dataRootPath}/workspace/small.png`, "b");
  secondPicture = open(`${dataRootPath}/workspace/big-picture.jpg`, "b");
} catch (e) {
  firstPicture = open(`${dataRootPath}/data/workspace/small.png`, "b");
  secondPicture = open(`${dataRootPath}/data/workspace/big-picture.jpg`, "b");
}

/** The size of a k6 response body, whichever shape it came back in. */
function bodyLength(body: any): number {
  if (body == null) {
    return 0;
  }
  if (typeof body === "string") {
    return body.length;
  }
  return typeof body.byteLength === "number" ? body.byteLength : 0;
}

/**
 * Polls the avatar route until it serves something, and until the extra condition holds. The caching runs
 * off the request, through the image resizer, so how long it takes is not ours to know — a fixed sleep
 * either flakes or wastes time.
 */
function awaitAvatar(userId: string, until?: (r: any) => boolean, timeoutSeconds = 30): any {
  for (let waited = 0; waited < timeoutSeconds; waited++) {
    const res = getAvatar(userId);
    if (res.status === 200 && bodyLength(res.body) > 0 && (until == null || until(res))) {
      return res;
    }
    sleep(1);
  }
  console.error(`Avatar never settled for ${userId} within ${timeoutSeconds}s`);
  return null;
}

export function setup(): StorageInitData {
  return initStorageFixture(schoolName);
}

export function testFindByFilenameEndingWith(data: StorageInitData) {
  describe('[Storage] List and drop the cached avatars of a user', () => {
    authenticateWeb(data.user.login);
    const userId = getConnectedUserId() as string;

    // First update: the cache is empty, so the listing matches nothing and only the caching runs.
    const uploaded = uploadFile(firstPicture, "avatar.png");
    let res = updateAvatar(userId, uploaded._id);
    let ok = check(res, {
      "the first avatar update should be ok": (r) => r.status === 200,
      "the first avatar update should echo the picture": (r) => {
        const picture = r.json("picture");
        return typeof picture === "string" && picture.indexOf(uploaded._id) >= 0;
      },
    });
    if (!ok) {
      console.error(`First avatar update failed: ${res.status} - ${res.body}`);
      return;
    }

    // The thumbnails are cached asynchronously, so poll rather than sleep a fixed amount.
    const first = awaitAvatar(userId);
    check(first, {
      "the avatar should be served after the first update": (r) => r != null && r.status === 200,
      "the served avatar should have content": (r) => r != null && bodyLength(r.body) > 0,
    });

    // Second update: this is the interesting one. The cache now holds objects whose keys end with the user
    // id, so findByFilenameEndingWith has to list them and removeFiles has to drop them. A ListObjectsV2
    // that came back rejected leaves the response a 200 all the same, so what the check below pins is that
    // the update completed and the avatar is still served afterwards.
    const replacement = uploadFile(secondPicture, "avatar-2.jpg");
    res = updateAvatar(userId, replacement._id);
    ok = check(res, {
      "the second avatar update should be ok": (r) => r.status === 200,
      "the second avatar update should echo the new picture": (r) => {
        const picture = r.json("picture");
        return typeof picture === "string" && picture.indexOf(replacement._id) >= 0;
      },
    });
    if (!ok) {
      console.error(`Second avatar update failed: ${res.status} - ${res.body}`);
      return;
    }

    const second = awaitAvatar(userId, (r) =>
        first == null || crypto.sha256(r.body as ArrayBuffer, "hex") !==
            crypto.sha256(first.body as ArrayBuffer, "hex"));
    check(second, {
      "the avatar should still be served after the replacement": (r) => r != null && r.status === 200,
      // The pin on findByFilenameEndingWith: the first picture had to be listed and removed for this to
      // be the second one. A listing that came back empty would leave the first avatar in place.
      "the served avatar should be the replacement, not the first one": (r) =>
          r != null && first != null &&
          crypto.sha256(r.body as ArrayBuffer, "hex") !== crypto.sha256(first.body as ArrayBuffer, "hex"),
    });
  });
}
