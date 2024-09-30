import { check } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import {
  authenticateWeb,
  createEmptyStructure
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";
import {
  listIsolated,
  getOrCreateUser,
  detachUserFromStructures,
  deleteOrPresuppressUsers
} from "../admin/utils.js";

chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    listIsolated: {
      exec: 'testListIsolated',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '5s',
    },
  },
};


function filterById(results, ids) {
  return results.filter( elem => ids.filter(id=>id===elem.id).length>0 );
}

/**
 * @returns A test dataset containing
 *  - admc: an ADMC user
 *  - users: a list of isolated users, without structure attachment
 */
export function setup() {
  let data;
  describe("[Isolated users] Initialize data", () => {
    const schoolName = `IT Isolated users`;
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    //////////////////////////////////
    // Create 1 structure and 4 users
    const structure = createEmptyStructure(schoolName, false, session);
    const user1 = getOrCreateUser(structure, "User1", "Isolated", "Personnel", session);
    detachUserFromStructures(user1, [structure], session);
    const user2 = getOrCreateUser(structure, "User2", "Isolated", "Teacher", session);
    detachUserFromStructures(user2, [structure], session);
    const user3 = getOrCreateUser(structure, "User3", "Isolated", "Personnel", session);
    detachUserFromStructures(user3, [structure], session);
    const user4 = getOrCreateUser(structure, "User4", "Isolated", "Relative", session);
    detachUserFromStructures(user4, [structure], session);

    data = {users: [user1, user2, user3, user4]};
  });
  return data;
}
/** Cleanup isolated users. */
export function teardown({users}) {
  const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
  if( users && session )
    deleteOrPresuppressUsers(users, session);
}

/**
 * Ensure that :
 * - isolated users are found
 * - isolated users are found in correct sort order
 * - code-injection is rejected when listing isolated users
 * @param {*} param0 Initialized data
 */
export function testListIsolated({users}) {
  describe("[Isolated users] List basic", () => {
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    tryListing(users, session);
    tryListingInDescOrder(session);
    tryInjectCode(session);
  });
};

function tryListing(users, session) {
  const requesterType = "ADMC";
  const userIds = users.map(u=>u.id);
  const results = listIsolated("+displayName", session);
  const checks = {};
  checks[`${requesterType} should be able to list all isolated users`] = () => {
    let ok;
    const isAnArray = Array.isArray(results);
    const foundUsers = isAnArray ? filterById(results,userIds) : 0;
    ok = isAnArray && foundUsers.length===4;
    if(!ok) {
      console.error(`------------------- ${requesterType} ----------------------`);
      console.error(`${requesterType} should be able to list all isolated users`);
      console.error('isAnArray', isAnArray);
      console.error('foundUsers', foundUsers.length);
      console.error(`------------------- End ----------------------`);
    }
    return ok;
  };
  return check(results, checks);
}

function tryListingInDescOrder(session) {
  const requesterType = "ADMC";
  const results = listIsolated("-displayName", session);
  const checks = {};
  checks[`${requesterType} should be able to list all isolated users in descending order`] = () => {
    let cause = true;
    if(Array.isArray(results)) {
      let previous;
      for(let result of results) {
        if( result.displayName != null ) {
          if(previous && result.displayName > previous) {
            cause = "order is not descending";
            break;
          }
          previous = result.displayName;
        }
      }
    } else {
      cause = "list is not an array";
    }
    if(typeof cause === "string") {
      console.error(`------------------- ${requesterType} ----------------------`);
      console.error(`${requesterType} should be able to list all isolated users in descending order`);
      console.error(`=> ${cause}.`);
      console.error(`------------------- End ----------------------`);
    }
    return typeof cause === "boolean";
  };
  return check(results, checks);
}

function tryInjectCode(session) {
  const requesterType = "ADMC";
  let requestAccepted = true;
  try {
    listIsolated("-(CASE WHEN MATCH (u:User{lastName:\"Isolated\"}) DETACH DELETE u RETURN true THEN \"displayName\" ELSE \"displayName\" END)", session);
  } catch {
    requestAccepted = false;
  }
  const checks = {};
  checks[`${requesterType} should not be able to inject code while listing`] = () => {
    const ok = !requestAccepted;
    if(!ok) {
      console.error(`------------------- ${requesterType} ----------------------`);
      console.error(`${requesterType} should not be able to inject code while listing`);
      console.error(`------------------- End ----------------------`);
    }
    return ok;
  };
  return check(requestAccepted, checks);
}
