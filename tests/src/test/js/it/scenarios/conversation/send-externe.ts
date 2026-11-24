import http from "k6/http";
import { check, sleep } from "k6";
import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  getUsersOfSchool,
  createAndSetRole,
  linkRoleToUsers,
  triggerImport,
  initStructure,
  getHeaders,
  lastNotifications,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

/*
 * POST /conversation/externe : messages sent by external applications, possibly without sender.
 *
 * Since usermessages_v2, user_id is a UUID : an empty "from" used to fail on the sender link. Such a message is now
 * only linked to its recipients ; the recipient must still list it, open it and be notified.
 *
 * The route is behind AdminFilter : run with an ADMC (or ADML) account.
 */

const rootUrl = __ENV.ROOT_URL;
const aafImport = (__ENV.AAF_IMPORT || "true") === "true";
const aafImportPause = parseInt(__ENV.AAF_IMPORT_PAUSE || "10");
const maxDuration = __ENV.MAX_DURATION || "2m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Conversation - Tests";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
/** The timeline notification is sent after the response : poll for it. */
const notificationTimeoutSeconds = parseInt(__ENV.NOTIFICATION_TIMEOUT || "10");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    sendExterneTest: {
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

export function setup() {
  let structure;
  describe("[Conversation-Externe-Init] Initialize data", () => {
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
  if (teachers.length < 2) throw "Not enough teachers in the structure";
  const [recipient, sender] = teachers;

  describe("[Conversation-Externe] Send from an external application", () => {
    testSenderlessMessageIsDelivered(recipient, "");
    testSenderlessMessageIsDelivered(recipient, undefined);
    testMessageWithSenderIsDelivered(recipient, sender);
    testMissingFieldsAreRejected(recipient);
  });
};

/* Scenarios */

/** An empty or missing "from" : delivered, listed, readable and notified, with no sender. */
function testSenderlessMessageIsDelivered(recipient, from: string | undefined) {
  const label = from === undefined ? "no from" : "empty from";
  const subject = `externe ${label} ${Date.now()}`;
  const res = sendExterne({ subject, messageBody: "<p>externe test</p>", to: [recipient.id], from, username: "External app" });
  check(res, { [`${label} - sent`]: (r) => r.status === 200 });

  const message = findInInbox(recipient, subject, label);
  if (!message) return;
  const read = http.get(`${rootUrl}/conversation/api/messages/${message.id}`, { headers: getHeaders() });
  check(read, {
    [`${label} - recipient opens the message`]: (r) => r.status === 200,
    [`${label} - opened message has the subject`]: (r) => r.status === 200 && JSON.parse(<string>r.body).subject === subject,
  });
  check(waitForNotification(recipient, message.id), {
    [`${label} - recipient is notified`]: (n) => !!n,
  });
}

/** Non regression : with a real sender, the message still goes through the usual send. */
function testMessageWithSenderIsDelivered(recipient, sender) {
  const subject = `externe with sender ${Date.now()}`;
  const res = sendExterne({ subject, messageBody: "<p>externe test</p>", to: [recipient.id], from: sender.id, username: "Sender" });
  check(res, { "with sender - sent": (r) => r.status === 200 });
  findInInbox(recipient, subject, "with sender");
}

function testMissingFieldsAreRejected(recipient) {
  const res = sendExterne({ messageBody: "<p>no subject</p>", to: [recipient.id], from: "" });
  check(res, { "Missing subject is rejected": (r) => r.status === 400 });
}

/* Helpers */

function sendExterne(payload) {
  authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  const headers = getHeaders();
  headers["content-type"] = "application/json";
  const res = http.post(`${rootUrl}/conversation/externe`, JSON.stringify(payload), { headers });
  if (res.status !== 200 && payload.subject) {
    console.error("POST /conversation/externe failed", res.status, res.body);
  }
  return res;
}

/** The route does not return the message id : the message is found in the inbox by its unique subject. */
function findInInbox(recipient, subject: string, label: string) {
  authenticateWeb(recipient.login);
  const res = http.get(`${rootUrl}/conversation/api/folders/inbox/messages?page=0&page_size=50`, { headers: getHeaders() });
  check(res, { [`${label} - recipient lists the inbox`]: (r) => r.status === 200 });
  const message = res.status === 200 ? JSON.parse(<string>res.body).find((m) => m.subject === subject) : undefined;
  check(message, { [`${label} - message is in the recipient inbox`]: (m) => !!m });
  return message;
}

function waitForNotification(recipient, messageId: string) {
  authenticateWeb(recipient.login);
  for (let waited = 0; waited < notificationTimeoutSeconds; waited++) {
    const found = (lastNotifications() || []).find((n) => n.type === "MESSAGERIE" && n.resource === messageId);
    if (found) return found;
    sleep(1);
  }
  return undefined;
}
