/**
 * referential POC, step 6: exercises the directory reads of conversation compared by its ACL layer
 * (list-users, list-users-structures, getGroupsInfos), then provokes a gap by writing a display name directly in
 * Neo4j (local recette only, no notification). The ACL logs the gaps on "referential.acl.diff" of the platform:
 * expected, none for the first reads, a "username" FIELD_MISMATCH for the sender after the direct write.
 *
 * Needs conversation with "referential-acl" configured, referential (REFERENTIAL_URL, default host of ROOT_URL on
 * port 8108) and the local Neo4j HTTP endpoint (NEO4J_URL, default host of ROOT_URL on port 7474, no auth).
 */
import { check, sleep } from "k6";
import http from "k6/http";
import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import {
  authenticateWeb,
  getUsersOfSchool,
  createAndSetRole,
  linkRoleToUsers,
  initStructure,
  getRandomUserWithProfile,
  DraftMessage,
  createDraftMessage,
  sendMessage,
  getHeaders,
  getProfileGroupOfStructureByType,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

const rootUrl = __ENV.ROOT_URL;
const host = (rootUrl || "http://localhost").replace(/^(https?:\/\/[^/:]+).*$/, "$1");
const referentialUrl = __ENV.REFERENTIAL_URL || `${host}:8108`;
const neo4jUrl = __ENV.NEO4J_URL || `${host}:7474`;
const schoolName = __ENV.DATA_SCHOOL_NAME || "Referential ACL - Tests";

export const options = {
  setupTimeout: "1h",
  thresholds: { checks: ["rate == 1.00"] },
  scenarios: {
    referentialAcl: { exec: "referentialAcl", executor: "per-vu-iterations", vus: 1, iterations: 1, maxDuration: "5m" },
  },
};

export function setup() {
  let structure;
  let users;
  describe("[Referential-ACL-Init] School, then referential reads it", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initStructure(schoolName);
    const role = createAndSetRole("Messagerie");
    linkRoleToUsers(structure, role, [`Teachers from group ${structure.name}.`, `Enseignants du groupe ${structure.name}.`]);
    users = getUsersOfSchool(structure);
    // imports are not notified: referential rereads the school as if it had replayed the files
    const res = http.post(`${referentialUrl}/api/resync/changed`, JSON.stringify({
      action: "k6-import", userIds: users.map((u) => u.id), structureIds: [structure.id], structureExternalIds: [],
      groupIds: [], subjectIds: [], positionIds: [], functions: false, groupMembers: false,
    }), { headers: { "Content-Type": "application/json" } });
    check(res, { "referential reads the school": (r) => r.status === 200 });
  });
  return { structure, users };
}

function readInboxAndMessage(messageId: string, step: string) {
  let res = http.get(`${rootUrl}/conversation/api/folders/inbox/messages?page=0&page_size=20`, { headers: getHeaders() });
  check(res, { [`${step} - inbox listed`]: (r) => r.status === 200 });
  res = http.get(`${rootUrl}/conversation/api/messages/${messageId}`, { headers: getHeaders() });
  check(res, { [`${step} - message read`]: (r) => r.status === 200 });
}

export function referentialAcl(data) {
  const { structure, users } = data;
  describe("[Referential-ACL] Directory reads of conversation compared with referential", () => {
    const sender = getRandomUserWithProfile(users, "Teacher");
    const recipient = getRandomUserWithProfile(users, "Teacher", [sender]);
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const teachers = getProfileGroupOfStructureByType("Teacher", structure);

    authenticateWeb(sender.login);
    const message = <DraftMessage>{ to: [recipient.id, teachers.id], cc: [], cci: [], subject: "ACL", body: "<div>ACL</div>" };
    let res = createDraftMessage(message);
    check(res, { "sender creates a draft": (r) => r.status === 201 });
    const messageId = JSON.parse(<string>res.body).id;
    res = sendMessage(messageId, message);
    check(res, { "sender sends the message": (r) => r.status === 200 });

    authenticateWeb(recipient.login);
    readInboxAndMessage(messageId, "before the direct write");
    sleep(2);

    // provoked gap: Neo4j changes without any notification, referential keeps the former display name
    res = http.post(`${neo4jUrl}/db/data/transaction/commit`, JSON.stringify({ statements: [{
      statement: "MATCH (u:User {id: {id}}) SET u.displayName = u.displayName + ' (ACL)'", parameters: { id: sender.id },
    }] }), { headers: { "Content-Type": "application/json" } });
    check(res, { "direct Neo4j write": (r) => r.status === 200 });
    readInboxAndMessage(messageId, "after the direct write");
    console.log(`expected gap: USER ${sender.id} FIELD_MISMATCH [username] on list-users`);
  });
}
