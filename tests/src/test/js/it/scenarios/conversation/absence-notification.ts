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
  DraftMessage,
  sendMessage,
  createDraftMessage,
  lastNotifications,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

/*
 * Timeline notification of the absence reply : the expeditor must be notified as if the absent user had
 * answered by hand.
 *
 * Replayability : the anti-spam slot (absent user, expeditor, day) survives the deactivation of the absence,
 * so a pair already used today gets no reply. Pairs are therefore picked among users with no notification
 * from each other yet ; on a long-lived environment the pool of fresh pairs shrinks, use a new
 * DATA_SCHOOL_NAME then.
 *
 * Prerequisite : the role "Messagerie - All - Stress Test" must hold the absence workflow right. createAndSetRole
 * reuses an existing role as is, so a role created before the absence feature lacks it (PUT absence -> 401).
 */

const rootUrl = __ENV.ROOT_URL;
const aafImport = (__ENV.AAF_IMPORT || "true") === "true";
const aafImportPause = parseInt(__ENV.AAF_IMPORT_PAUSE || "10");
const maxDuration = __ENV.MAX_DURATION || "2m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Conversation - Tests";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");
/** Replies go through the bus, in slices with a pause : they are never readable right after the send. */
const replyTimeoutSeconds = parseInt(__ENV.ABSENCE_REPLY_TIMEOUT || "20");
/** How long to wait before asserting that no notification came. */
const quietSeconds = parseInt(__ENV.ABSENCE_QUIET_DELAY || "8");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    absenceNotificationTest: {
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

export function setup() {
  let structure;
  describe("[Conversation-Absence-Init] Initialize data", () => {
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
  const used = [];
  const absent = pickUnused(teachers, used);
  const expeditors = [];
  for (let i = 0; i < 4; i++) {
    expeditors.push(pickFreshExpeditor(teachers, used, absent));
  }
  const [b1, b2, b3, b4] = expeditors;

  describe("[Conversation-Absence] Notification of the absence reply", () => {
    setAbsence(absent, true);
    try {
      testReplyIsNotifiedAsAManualAnswer(absent, b1);
      testSimultaneousExpeditorsAreAllNotified(absent, b2, b3);
    } finally {
      setAbsence(absent, false);
    }
    testDisabledAbsenceIsNotNotified(absent, b4);
  });
};

/* Scenarios */

/** Cases 1, 2 and 4 of the plan : the automatic notification against the one of a manual answer. */
function testReplyIsNotifiedAsAManualAnswer(absent, b) {
  sendAs(b, absent, "absence test - first message");
  const auto = waitForNotifications(b, absent, 1);
  check(auto, { "Expeditor is notified of the absence reply": (n) => n.length === 1 });
  if (auto.length !== 1) return;
  const params = <any>auto[0].params;
  check(params, {
    "Notification carries the absent user name": (p) => !!p.username && p.username !== "null",
    "Directory link carries the absent user profile": (p) => p.uri === `/userbook/annuaire#${absent.id}#Teacher`,
    "Notification links the reply": (p) => !!p.messageUri && p.messageUri.endsWith(`/read-mail/${auto[0].resource}`),
  });

  // Reference : the absent user answers by hand, through the send route.
  sendAs(absent, b, "absence test - manual answer");
  const both = waitForNotifications(b, absent, 2);
  check(both, { "Expeditor is notified of the manual answer": (n) => n.length === 2 });
  if (both.length === 2) {
    const manual = both.find((n) => n._id !== auto[0]._id);
    const m = <any>manual.params;
    check(manual, {
      "Same notification type as a manual answer": (n) =>
        n.type === auto[0].type && n["event-type"] === auto[0]["event-type"] && n.sender === auto[0].sender,
      "Same sender name as a manual answer": () => m.username === params.username,
      "Same directory link as a manual answer": () => m.uri === params.uri,
    });
  }

  // The daily slot is spent : a second message gets no reply, hence no notification.
  sendAs(b, absent, "absence test - second message");
  sleep(quietSeconds);
  check(notificationsFrom(b, absent), {
    "Second message of the day is not notified": (n) => n.length === 2,
  });
}

/**
 * Case 3 : the timeline anti-flood is keyed by sender, for 3 s by default. Two expeditors writing back to back
 * get two replies from the same sender within that delay : without disableAntiFlood the second notification
 * would be silently dropped.
 */
function testSimultaneousExpeditorsAreAllNotified(absent, b2, b3) {
  const start = Date.now();
  sendAs(b2, absent, "absence test - simultaneous expeditor 1");
  sendAs(b3, absent, "absence test - simultaneous expeditor 2");
  const elapsed = (Date.now() - start) / 1000;
  if (elapsed > 3) {
    console.warn(`Sends spread over ${elapsed}s : beyond the anti-flood delay, this case proves less.`);
  }
  check(waitForNotifications(b2, absent, 1), { "First simultaneous expeditor is notified": (n) => n.length === 1 });
  check(waitForNotifications(b3, absent, 1), { "Second simultaneous expeditor is notified": (n) => n.length === 1 });
}

/** Case 5 : no absence, no reply, no notification. */
function testDisabledAbsenceIsNotNotified(absent, b) {
  sendAs(b, absent, "absence test - after deactivation");
  sleep(quietSeconds);
  check(notificationsFrom(b, absent), {
    "Disabled absence is not notified": (n) => n.length === 0,
  });
}

/* Helpers */

function setAbsence(user, enabled: boolean) {
  authenticateWeb(user.login);
  const headers = getHeaders();
  headers["content-type"] = "application/json";
  const now = Date.now();
  const payload = {
    enabled,
    startAt: new Date(now - 3600 * 1000).toISOString(),
    endAt: new Date(now + 24 * 3600 * 1000).toISOString(),
    bodyJson: {
      type: "doc",
      content: [{ type: "paragraph", content: [{ type: "text", text: "Absent, back tomorrow." }] }],
    },
  };
  const res = http.put(`${rootUrl}/conversation/absence`, JSON.stringify(payload), { headers });
  const ok = check(res, { [`Absence ${enabled ? "enabled" : "disabled"} - HTTP status`]: (r) => r.status === 200 });
  if (!ok) {
    console.error("PUT /conversation/absence failed (absence right missing from the role ?)", res.status, res.body);
  }
}

function sendAs(from, to, subject: string) {
  authenticateWeb(from.login);
  const draft = <DraftMessage>{ to: [to.id], cc: [], cci: [], subject, body: "<p>absence test</p>" };
  const res = createDraftMessage(draft);
  check(res, { [`${subject} - draft created`]: (r) => r.status === 201 });
  const sent = sendMessage(JSON.parse(<string>res.body).id, draft);
  check(sent, { [`${subject} - sent`]: (r) => r.status === 200 });
}

/** Messaging notifications of the given recipient whose sender is the given user. */
function notificationsFrom(recipient, sender) {
  authenticateWeb(recipient.login);
  return (lastNotifications() || []).filter((n) => n.type === "MESSAGERIE" && n.sender === sender.id);
}

function waitForNotifications(recipient, sender, expected: number) {
  let found = [];
  for (let waited = 0; waited < replyTimeoutSeconds; waited++) {
    found = notificationsFrom(recipient, sender);
    if (found.length >= expected) break;
    sleep(1);
  }
  return found;
}

function pickUnused(users, used) {
  const user = users.find((u) => !used.includes(u.id));
  if (!user) throw "Not enough teachers in the structure";
  used.push(user.id);
  return user;
}

/** An expeditor that never got a notification from the absent user, hence no reply slot spent with him. */
function pickFreshExpeditor(users, used, absent) {
  for (const user of users) {
    if (used.includes(user.id)) continue;
    used.push(user.id);
    if (notificationsFrom(user, absent).length === 0) return user;
  }
  throw `No fresh expeditor left for ${absent.id} : use a new DATA_SCHOOL_NAME`;
}
