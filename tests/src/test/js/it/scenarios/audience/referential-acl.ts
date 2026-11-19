/**
 * referential POC, step 8 (second module): exercises the directory read of audience compared by its ACL layer
 * (get-users-displayNames on entcore.directory) through the reactions of a news item (actualites), then provokes a
 * gap by writing the display name directly in Neo4j (local recette only, no notification, restored at the end).
 * The ACL logs the gaps on "referential.acl.diff": expected, none before the direct write, a "displayName"
 * FIELD_MISMATCH for the reacting user after it.
 *
 * Needs audience with "referential-acl" configured, the actualites application, referential (REFERENTIAL_URL,
 * default host of ROOT_URL on port 8108) and the local Neo4j HTTP endpoint (NEO4J_URL, default port 7474).
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
  getHeaders,
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

function json() {
  const headers = getHeaders();
  headers["content-type"] = "application/json";
  return headers;
}

function neo4j(statement: string, id: string) {
  return http.post(`${neo4jUrl}/db/data/transaction/commit`, JSON.stringify({ statements: [{ statement, parameters: { id } }] }),
    { headers: { "Content-Type": "application/json" } });
}

export function setup() {
  let structure;
  let users;
  describe("[Referential-ACL-Audience-Init] School with the news role, then referential reads it", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = initStructure(schoolName);
    const role = createAndSetRole("Actualites");
    linkRoleToUsers(structure, role, [`Teachers from group ${structure.name}.`, `Enseignants du groupe ${structure.name}.`]);
    users = getUsersOfSchool(structure);
    const res = http.post(`${referentialUrl}/api/resync/changed`, JSON.stringify({
      action: "k6-import", userIds: users.map((u) => u.id), structureIds: [structure.id], structureExternalIds: [],
      groupIds: [], subjectIds: [], positionIds: [], functions: false, groupMembers: false,
    }), { headers: { "Content-Type": "application/json" } });
    check(res, { "referential reads the school": (r) => r.status === 200 });
  });
  return { structure, users };
}

function readReactionDetails(infoId: string, step: string) {
  const res = http.get(`${rootUrl}/audience/reactions/actualites/info/${infoId}?page=1&size=10`, { headers: getHeaders() });
  check(res, { [`${step} - reaction details read`]: (r) => r.status === 200 });
}

export function referentialAcl(data) {
  const { structure, users } = data;
  describe("[Referential-ACL-Audience] Display names of audience compared with referential", () => {
    const teacher = getRandomUserWithProfile(users, "Teacher");
    authenticateWeb(teacher.login);

    let res = http.post(`${rootUrl}/actualites/api/v1/threads`, JSON.stringify({ title: "ACL", mode: 0, structure: { id: structure.id } }),
      { headers: json() });
    check(res, { "teacher creates a thread": (r) => r.status === 200 || r.status === 201 });
    const threadId = JSON.parse(<string>res.body).id;
    res = http.post(`${rootUrl}/actualites/api/v1/infos/published`, JSON.stringify({ title: "ACL", content: "<p>ACL</p>", thread_id: threadId }),
      { headers: json() });
    check(res, { "teacher publishes a news item": (r) => r.status === 200 || r.status === 201 });
    const infoId = String(JSON.parse(<string>res.body).id);
    res = http.post(`${rootUrl}/audience/reactions/actualites/info`, JSON.stringify({ resourceId: infoId, reactionType: "REACTION_1" }),
      { headers: json() });
    if (!check(res, { "teacher reacts": (r) => r.status === 200 || r.status === 201 })) {
      console.error(`reaction refused: HTTP ${res.status}`);
    }

    readReactionDetails(infoId, "before the direct write");
    sleep(2);

    // provoked gap: Neo4j changes without any notification, referential keeps the former display name
    res = neo4j("MATCH (u:User {id: {id}}) SET u.displayName = u.displayName + ' (ACL)'", teacher.id);
    check(res, { "direct Neo4j write": (r) => r.status === 200 });
    readReactionDetails(infoId, "after the direct write");
    sleep(2);
    res = neo4j("MATCH (u:User {id: {id}}) WHERE u.displayName ENDS WITH ' (ACL)' "
      + "SET u.displayName = substring(u.displayName, 0, size(u.displayName) - 6)", teacher.id);
    check(res, { "display name restored": (r) => r.status === 200 });
    console.log(`expected gap: USER ${teacher.id} FIELD_MISMATCH [displayName] on get-users-displayNames`);
  });
}
