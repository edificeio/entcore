import http from "k6/http";
import { check, fail, sleep } from "k6";
import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import { FormData } from "https://jslib.k6.io/formdata/0.0.2/index.js";

import {
  authenticateWeb,
  getUsersOfSchool,
  createAndSetRole,
  linkRoleToUsers,
  triggerImport,
  initStructure,
  getHeaders,
  DraftMessage,
  sendMessage,
  createDraftMessage,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

/*
 * Storage of the recipients when a message is sent : every recipient is charged the size of the attachments,
 * in a single batched update of the workspace quota service, the sender excluded (already charged at upload).
 *
 * Replayable : storages are compared before and after each send, never against absolute values.
 * The quota update runs after the send response : expected storages are polled, up to QUOTA_TIMEOUT seconds.
 */

const rootUrl = __ENV.ROOT_URL;
const aafImport = (__ENV.AAF_IMPORT || "true") === "true";
const aafImportPause = parseInt(__ENV.AAF_IMPORT_PAUSE || "10");
const maxDuration = __ENV.MAX_DURATION || "2m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Conversation - Tests";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
const recipientsCount = parseInt(__ENV.QUOTA_RECIPIENTS || "5");
const quotaTimeoutSeconds = parseInt(__ENV.QUOTA_TIMEOUT || "10");
/** How long to wait before asserting that a storage did not move. */
const quietSeconds = parseInt(__ENV.QUOTA_QUIET_DELAY || "3");
const ATTACHMENT = "x".repeat(2048);

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    sendQuotaTest: {
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

export function setup() {
  let structure;
  describe("[Conversation-Quota-Init] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initStructure(schoolName);
    const role = createAndSetRole("Messagerie");
    linkRoleToUsers(structure, role, [
      `Teachers from group ${structure.name}.`,
      `Enseignants du groupe ${structure.name}.`,
    ]);
  });
  if (aafImport) {
    triggerImport();
    sleep(aafImportPause);
  }
  return { structure };
}

export default (data) => {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  const teachers = getUsersOfSchool(data.structure).filter((u) => u.type === "Teacher");
  if (teachers.length < recipientsCount + 1) {
    fail(`${recipientsCount + 1} teachers needed, ${teachers.length} found in ${data.structure.name}`);
  }
  const [sender, ...others] = teachers;
  const recipients = others.slice(0, recipientsCount);

  describe("[Conversation-Quota] Storage of the recipients on send", () => {
    testAttachmentIsChargedToEveryRecipient(sender, recipients);
    testRecipientTwiceIsChargedOnce(sender, recipients[0]);
    testMessageWithoutAttachmentChangesNoStorage(sender, recipients);
  });
};

/* Scenarios */

function testAttachmentIsChargedToEveryRecipient(sender, recipients) {
  const before = storagesOf(recipients);
  const draft = <DraftMessage>{ to: recipients.map((r) => r.id), cc: [], cci: [], subject: "quota test - attachment", body: "<p>quota</p>" };
  const size = sendWithAttachment(sender, draft);
  const after = waitForStorages(recipients, before, size);
  recipients.forEach((r, i) => {
    check(after[i], { [`Recipient ${i + 1} is charged the attachment size (${size} bytes)`]: (s) => s === before[i] + size });
  });
}

/** The recipient list is a set : appearing in to and cc must not charge the attachment twice. */
function testRecipientTwiceIsChargedOnce(sender, recipient) {
  const [before] = storagesOf([recipient]);
  const draft = <DraftMessage>{ to: [recipient.id], cc: [recipient.id], cci: [], subject: "quota test - to and cc", body: "<p>quota</p>" };
  const size = sendWithAttachment(sender, draft);
  waitForStorages([recipient], [before], size);
  sleep(quietSeconds);
  const [after] = storagesOf([recipient]);
  check(after, { "Recipient in to and cc is charged once": (s) => s === before + size });
}

function testMessageWithoutAttachmentChangesNoStorage(sender, recipients) {
  const before = storagesOf(recipients);
  const draft = <DraftMessage>{ to: recipients.map((r) => r.id), cc: [], cci: [], subject: "quota test - no attachment", body: "<p>quota</p>" };
  authenticateWeb(sender.login);
  const res = createDraftMessage(draft);
  check(res, { "No attachment - draft created": (r) => r.status === 201 });
  check(sendMessage(JSON.parse(<string>res.body).id, draft), { "No attachment - sent": (r) => r.status === 200 });
  sleep(quietSeconds);
  const after = storagesOf(recipients);
  check(after, { "Message without attachment changes no storage": (s) => s.every((value, i) => value === before[i]) });
}

/* Helpers */

/**
 * Creates the draft, attaches a file and sends it.
 * @returns the size actually charged to the sender at upload, the one every recipient must be charged
 */
function sendWithAttachment(sender, draft: DraftMessage): number {
  const [senderBefore] = storagesOf([sender]);
  authenticateWeb(sender.login);
  const res = createDraftMessage(draft);
  check(res, { [`${draft.subject} - draft created`]: (r) => r.status === 201 });
  const draftId = JSON.parse(<string>res.body).id;

  const fd = new FormData();
  fd.append("file", http.file(ATTACHMENT, "quota.txt", "text/plain"));
  const headers = getHeaders();
  //@ts-ignore
  headers["Content-Type"] = "multipart/form-data; boundary=" + fd.boundary;
  //@ts-ignore
  const upload = http.post(`${rootUrl}/conversation/message/${draftId}/attachment`, fd.body(), { headers });
  check(upload, { [`${draft.subject} - attachment uploaded`]: (r) => r.status === 200 });

  const [senderAfterUpload] = storagesOf([sender]);
  const size = senderAfterUpload - senderBefore;
  check(size, { [`${draft.subject} - sender is charged at upload`]: (s) => s > 0 });

  authenticateWeb(sender.login);
  check(sendMessage(draftId, draft), { [`${draft.subject} - sent`]: (r) => r.status === 200 });
  sleep(quietSeconds);
  const [senderAfterSend] = storagesOf([sender]);
  check(senderAfterSend, { [`${draft.subject} - sender is not charged again on send`]: (s) => s === senderAfterUpload });
  return size;
}

/** Storage of each user, read as ADMC from the workspace quota service. */
function storagesOf(users): number[] {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  return users.map((u) => {
    const res = http.get(`${rootUrl}/workspace/quota/user/${u.id}`, { headers: getHeaders() });
    if (res.status !== 200) {
      fail(`Could not read the quota of ${u.id} (${res.status}): ${res.body}`);
    }
    const storage = JSON.parse(<string>res.body).storage;
    if (typeof storage !== "number") {
      fail(`User ${u.id} has no storage counter yet (not activated ?): ${res.body}`);
    }
    return storage;
  });
}

function waitForStorages(users, before: number[], size: number): number[] {
  let storages = storagesOf(users);
  for (let waited = 0; waited < quotaTimeoutSeconds; waited++) {
    if (storages.every((s, i) => s >= before[i] + size)) break;
    sleep(1);
    storages = storagesOf(users);
  }
  return storages;
}
