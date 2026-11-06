import { check, sleep } from "k6";
import http from "k6/http";
import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";

import {
  authenticateWeb,
  getUsersOfSchool,
  getRandomUserWithProfile,
  createAndSetRole,
  linkRoleToUsers,
  initStructure,
  createDraftMessage,
  sendMessage,
  getHeaders,
  DraftMessage,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

const rootUrl = __ENV.ROOT_URL;
const maxDuration = __ENV.MAX_DURATION || "2m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Timeline - Tests";
const gracefulStop = __ENV.GRACEFUL_STOP || "2s";
const HOUR = 3600 * 1000;

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    notificationsSince: {
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

export function setup() {
  let structure;
  describe("[Timeline-Init] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initStructure(schoolName, "tiny");
    const role = createAndSetRole("Messagerie");
    linkRoleToUsers(structure, role, [
      `Teachers from group ${structure.name}.`,
      `Enseignants du groupe ${structure.name}.`,
    ]);
  });
  return { structure };
}

export default (data) => {
  testNotificationsSince(data);
  testNotificationsSinceBadRequest(data);
  testNotificationsSinceNotAuthenticated();
};

function getNotificationsSince(date: string | null) {
  const query = date === null ? "" : `?date=${encodeURIComponent(date)}`;
  return http.get(`${rootUrl}/timeline/notifications/since${query}`, {
    headers: getHeaders(),
  });
}

/** Envoie un message : le destinataire reçoit une notification timeline dont resource = id du message. */
function sendTestMessage(from, to, subject: string): string {
  authenticateWeb(from.login);
  const draft = <DraftMessage>{
    to: [to.id],
    cc: [],
    cci: [],
    subject,
    body: "Notification timeline",
  };
  const created = createDraftMessage(draft);
  check(created, { [`${subject} - draft created`]: (r) => r.status === 201 });
  const messageId = JSON.parse(created.body).id;
  const sent = sendMessage(messageId, draft);
  check(sent, { [`${subject} - message sent`]: (r) => r.status === 200 });
  return messageId;
}

/** La notification est créée de façon asynchrone : on réessaie jusqu'à voir toutes les ressources attendues. */
function waitForNotifications(since: string, expectedResources: string[]) {
  let res;
  let results = [];
  for (let i = 0; i < 20; i++) {
    res = getNotificationsSince(since);
    if (res.status === 200) {
      results = res.json("results");
      if (expectedResources.every((id) => results.some((n) => n.resource === id))) {
        break;
      }
    }
    sleep(0.5);
  }
  return { res, results };
}

function testNotificationsSince(data) {
  const { structure } = data;
  describe("[Timeline] Notifications received since a date", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(structure);
    const receiver = getRandomUserWithProfile(users, "Teacher");
    const sender1 = getRandomUserWithProfile(users, "Teacher", [receiver]);
    // Deux émetteurs distincts : l'anti-flood du timeline ignore les notifications
    // d'un même émetteur envoyées à moins de quelques secondes d'intervalle.
    const sender2 = getRandomUserWithProfile(users, "Teacher", [receiver, sender1]);

    const since = new Date(Date.now() - HOUR).toISOString();
    const messageId1 = sendTestMessage(sender1, receiver, "Timeline since 1");
    sleep(1);
    const messageId2 = sendTestMessage(sender2, receiver, "Timeline since 2");

    // Le destinataire voit les deux notifications
    authenticateWeb(receiver.login);
    const { res, results } = waitForNotifications(since, [messageId1, messageId2]);
    check(res, {
      "Receiver - HTTP status": (r) => r.status === 200,
      "Receiver - status ok": (r) => r.json("status") === "ok",
    });
    check(results, {
      "Receiver - sees both notifications": (r) =>
        [messageId1, messageId2].every((id) => r.some((n) => n.resource === id)),
      "Receiver - notifications are of type MESSAGERIE / SEND-MESSAGE": (r) =>
        r
          .filter((n) => [messageId1, messageId2].includes(n.resource))
          .every((n) => n.type === "MESSAGERIE" && n["event-type"] === "SEND-MESSAGE"),
      "Receiver - only resource, date, type and event-type are returned": (r) =>
        r.every((n) => Object.keys(n).sort().join(",") === "date,event-type,resource,type"),
      "Receiver - newest first": (r) =>
        r.every(
          (n, i) =>
            i === 0 ||
            new Date(n.date.$date).getTime() <= new Date(r[i - 1].date.$date).getTime(),
        ),
    });

    // Une date dans le futur ne remonte rien
    const future = getNotificationsSince(new Date(Date.now() + HOUR).toISOString());
    check(future, {
      "Future date - HTTP status": (r) => r.status === 200,
      "Future date - no notification": (r) => r.json("results").length === 0,
    });

    // Les émetteurs ne reçoivent pas ces notifications
    authenticateWeb(sender1.login);
    const forSender = getNotificationsSince(since);
    check(forSender, {
      "Sender - HTTP status": (r) => r.status === 200,
      "Sender - does not see the receiver's notifications": (r) =>
        !r.json("results").some((n) => n.resource === messageId1 || n.resource === messageId2),
    });
  });
}

function testNotificationsSinceBadRequest(data) {
  const { structure } = data;
  describe("[Timeline] Notifications since - invalid date", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const user = getRandomUserWithProfile(getUsersOfSchool(structure), "Teacher");
    authenticateWeb(user.login);
    check(getNotificationsSince(null), { "Missing date - HTTP 400": (r) => r.status === 400 });
    check(getNotificationsSince("hier"), { "Invalid date - HTTP 400": (r) => r.status === 400 });
    check(getNotificationsSince("2026-01-01T00:00:00"), {
      "Date without offset - HTTP 400": (r) => r.status === 400,
    });
  });
}

function testNotificationsSinceNotAuthenticated() {
  describe("[Timeline] Notifications since - not authenticated", () => {
    // Jar vide : aucune session n'est envoyée
    const res = http.get(
      `${rootUrl}/timeline/notifications/since?date=${encodeURIComponent(new Date(Date.now() - HOUR).toISOString())}`,
      { jar: new http.CookieJar() },
    );
    check(res, {
      "Not authenticated - redirected to login or refused": (r) => r.status === 302 || r.status === 401,
    });
  });
}
