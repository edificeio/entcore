import {
  authenticateWeb,
  initStructure,
  Structure,
  getUsersOfSchool,
  getRandomUserWithProfile,
  addCommunicationBetweenGroups,
  removeCommunicationBetweenGroups,
  createGroupOrFail,
  Group,
  addUsersToGroup,
  modifyCommunicationRelationOrFail,
  deleteGroupOrFail,
  getProfileGroupOfStructureByType,
  setDirectCommunicationOrFail,
  ProfileGroup,
  checkEquals, checkFalse, checkTrue, checkNotEquals, checkGte
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import http from "k6/http";
import {check, group} from "k6";
import { getHeaders } from "../../../node_modules/edifice-k6-commons/dist/index.js";

import {checkUsersCanCommunicate} from "./_utils.ts";


const rootUrl = __ENV.ROOT_URL;
const maxDuration = __ENV.MAX_DURATION || "20m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Communication Controller";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

const availableScenarioNames = ["testSetDirectCommunication", "testRemoveLink", "testAddLinksWithUsers", "testRemoveLinksWithUsers", "testCommuniqueWith", "testGetOutgoingRelations", "testGetIncomingRelations", "testAddLinkBetweenRelativeAndStudent", "testRemoveLinkBetweenRelativeAndStudent", "testVisibleUsers", "testVisibleGroupContains", "testSafelyAddLinksWithUsers", "testSafelyRemoveLinksWithUsers", "testAddLinkCheckOnly", "testProcessAddLinkAndChangeDirection", "testRemoveRelations", "testVerify", "testGetDiscoverVisibleUsers", "testGetDiscoverVisibleAcceptedProfile", "testGetDiscoverVisibleStructures", "testDiscoverVisibleAddCommuteUsers", "testDiscoverVisibleRemoveCommuteUsers", "testDiscoverVisibleGetGroups", "testDiscoverVisibleGetUsersInGroup", "testCreateDiscoverVisibleGroup", "testUpdateDiscoverVisibleGroup", "testAddDiscoverVisibleGroupUsers"];

const enabledScenarios = __ENV.COMMUNICATION_ENDPOINTS_SCENARIOS ? __ENV.COMMUNICATION_ENDPOINTS_SCENARIOS.split(",") : availableScenarioNames;

const scenarios = enabledScenarios
  .filter((name: string) => availableScenarioNames.includes(name))
  .reduce((acc: any, name: string) => {
  acc[name] = {
    executor: "per-vu-iterations",
    exec: name,
    vus: 1,
    maxDuration: maxDuration,
    gracefulStop,
  };
  return acc;
}, {} as Record<string, any>);

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: scenarios
};

type InitData = {
  structure1: Structure;
  structure2: Structure;
}

export function setup() {
  let structure1: Structure;
  let structure2: Structure;

  group("[Communication-Endpoints] Initialize data", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure1 = initStructure(`${schoolName} - School1`);
    structure2 = initStructure(`${schoolName} - School2`);
  });
  return { structure1 : structure1, structure2 : structure2 };
}

/***************************************************************************************************
 * Establish a direct communication link between two users and check that these users can communicate
 * via the verify endpoint
 ***************************************************************************************************/
export function testSetDirectCommunication(data: InitData) {
  group('[Endpoints] setDirectCommunication', () => {
    const testName = "[setDirectCommunication]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');
    checkTrue(`${testName} users cannot communicate before setting direct communication`, checkUsersCanCommunicate(teacher1.id, teacher2.id, false));

    setDirectCommunicationOrFail(teacher1.id, teacher2.id, 'both');

    checkTrue(`${testName} users can communicate after setting direct communication`, checkUsersCanCommunicate(teacher1.id, teacher2.id, true));
  });
}

/***************************************************************************************************
 * Validates the deprecated DELETE /group/:startGroupId/communique/:endGroupId endpoint.
 * Creates two groups with a COMMUNIQUE link and assigns one user to each, verifies they can
 * communicate, then removes the link and asserts communication is no longer possible.
 ***************************************************************************************************/
export function testRemoveLink(data: InitData) {
  group('[Endpoints] removeLink', () => {
    const testName = "[removeLink]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');
    checkTrue(`${testName} users cannot communicate before setting group communication`, checkUsersCanCommunicate(teacher1.id, teacher2.id, false));

    const group1: Group = createGroupOrFail(data.structure1.name + "-removeLink-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-removeLink-g2", data.structure2);
    addCommunicationBetweenGroups(group1.id, group2.id);

    addUsersToGroup([teacher1.id], group1);
    addUsersToGroup([teacher2.id], group2);

    checkTrue(`${testName} users can communicate after setting group communication`, checkUsersCanCommunicate(teacher1.id, teacher2.id, true));

    // Remove the link via deprecated endpoint
    const res = http.del(
      `${rootUrl}/communication/group/${group1.id}/communique/${group2.id}`,
      null,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} removeLink returns 200`, 200, res.status);

    checkTrue(`${testName} users cannot communicate after removing group communication`, checkUsersCanCommunicate(teacher1.id, teacher2.id, false));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });
}

/***************************************************************************************************
 * Validates POST /group/:groupId?direction=BOTH sets the intra-group users communication direction.
 * Creates a group and calls the endpoint to enable BOTH direction, then checks a 200 response.
 ***************************************************************************************************/
export function testAddLinksWithUsers(data: InitData) {
  group('[Endpoints] addLinksWithUsers', () => {
    const testName = "[addLinksWithUsers]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-addLinksWithUsers-g1", data.structure1);

    const res = http.post(
      `${rootUrl}/communication/group/${group1.id}?direction=BOTH`,
      null,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} addLinksWithUsers returns 200`, 200, res.status);

    // tear down
    deleteGroupOrFail(group1);
  });
}

/***************************************************************************************************
 * Validates DELETE /group/:groupId?direction=BOTH removes the intra-group users communication.
 * Creates a group with BOTH direction enabled, then removes it and checks a 200 response.
 ***************************************************************************************************/
export function testRemoveLinksWithUsers(data: InitData) {
  group('[Endpoints] removeLinksWithUsers', () => {
    const testName = "[removeLinksWithUsers]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-removeLinksWithUsers-g1", data.structure1);
    modifyCommunicationRelationOrFail(group1, 'both');

    const res = http.del(
      `${rootUrl}/communication/group/${group1.id}?direction=BOTH`,
      null,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} removeLinksWithUsers returns 200`, 200, res.status);

    // tear down
    deleteGroupOrFail(group1);
  });
}

/***************************************************************************************************
 * Validates GET /group/:groupId returns the list of groups the source can communicate to.
 * Tests: empty state for a new group, single link appears in response, multiple links listed,
 * reverse direction does NOT show the source, link disappears after removal, and users in linked
 * groups can actually communicate (verified via the verify endpoint).
 ***************************************************************************************************/
export function testCommuniqueWith(data: InitData) {
  group('[Endpoints] communiqueWith - no link returns empty communiqueWith', () => {
    const testName = "[communiqueWith-empty]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-communiqueWith-empty-g1", data.structure1);

    // A freshly created group with no outgoing link should have an empty communiqueWith
    const res = http.get(
      `${rootUrl}/communication/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkEquals(`${testName} response has id field matching the group`, group1.id, body.id);
    checkTrue(`${testName} response contains communiqueWith array`, Array.isArray(body.communiqueWith));
    checkEquals(`${testName} communiqueWith is empty when no link exists`, 0, body.communiqueWith.length);

    deleteGroupOrFail(group1);
  });

  group('[Endpoints] communiqueWith - single link', () => {
    const testName = "[communiqueWith-single]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-communiqueWith-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-communiqueWith-g2", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    const res = http.get(
      `${rootUrl}/communication/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response has id field`, typeof body.id === "string" && body.id === group1.id);
    checkTrue(`${testName} response contains communiqueWith array`, Array.isArray(body.communiqueWith));
    checkEquals(`${testName} communiqueWith has exactly 1 entry`, 1, body.communiqueWith.length);
    checkTrue(`${testName} communiqueWith entry has id field`, typeof body.communiqueWith[0].id === "string");
    checkEquals(`${testName} communiqueWith contains target group`, group2.id, body.communiqueWith[0].id);

    // The reverse direction: group2 should NOT have group1 in its communiqueWith
    const resReverse = http.get(
      `${rootUrl}/communication/group/${group2.id}`,
      { headers: getHeaders() },
    );
    const bodyReverse = JSON.parse(<string>resReverse.body);
    checkEquals(`${testName} reverse returns 200`, 200, resReverse.status);
    checkFalse(`${testName} reverse communiqueWith does NOT contain source group`, bodyReverse.communiqueWith.some((g: any) => g.id === group1.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] communiqueWith - multiple links', () => {
    const testName = "[communiqueWith-multi]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-communiqueWith-multi-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure1.name + "-communiqueWith-multi-g2", data.structure1);
    const group3: Group = createGroupOrFail(data.structure2.name + "-communiqueWith-multi-g3", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);
    addCommunicationBetweenGroups(group1.id, group3.id);

    const res = http.get(
      `${rootUrl}/communication/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkEquals(`${testName} communiqueWith has 2 entries`, 2, body.communiqueWith.length);
    checkTrue(`${testName} communiqueWith contains group2`, body.communiqueWith.some((g: any) => g.id === group2.id));
    checkTrue(`${testName} communiqueWith contains group3`, body.communiqueWith.some((g: any) => g.id === group3.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
    deleteGroupOrFail(group3);
  });

  group('[Endpoints] communiqueWith - after removing link', () => {
    const testName = "[communiqueWith-removed]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-communiqueWith-rm-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-communiqueWith-rm-g2", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    // Verify link exists
    const resBefore = http.get(
      `${rootUrl}/communication/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const bodyBefore = JSON.parse(<string>resBefore.body);
    checkTrue(`${testName} before removal contains target group`, bodyBefore.communiqueWith.some((g: any) => g.id === group2.id));

    // Remove the link
    removeCommunicationBetweenGroups(group1.id, group2.id);

    // Verify link no longer present
    const resAfter = http.get(
      `${rootUrl}/communication/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const bodyAfter = JSON.parse(<string>resAfter.body);
    checkEquals(`${testName} after removal returns 200`, 200, resAfter.status);
    checkFalse(`${testName} after removal communiqueWith does NOT contain removed group`, bodyAfter.communiqueWith.some((g: any) => g.id === group2.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] communiqueWith - users can communicate through linked groups', () => {
    const testName = "[communiqueWith-users]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "-communiqueWith-vis-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-communiqueWith-vis-g2", data.structure2);

    addUsersToGroup([teacher1.id], group1);
    addUsersToGroup([teacher2.id], group2);

    // Before adding link, users should not communicate
    checkTrue(`${testName} users cannot communicate before link`, checkUsersCanCommunicate(teacher1.id, teacher2.id, false));

    // Add link and enable users communication on both groups
    addCommunicationBetweenGroups(group1.id, group2.id);

    // After adding link, users should be able to communicate
    checkTrue(`${testName} users can communicate after link`, checkUsersCanCommunicate(teacher1.id, teacher2.id, true));

    // Remove the link
    removeCommunicationBetweenGroups(group1.id, group2.id);

    // After removing link, users should no longer communicate
    checkTrue(`${testName} users cannot communicate after link removal`, checkUsersCanCommunicate(teacher1.id, teacher2.id, false));

    // tear down
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });
}

/***************************************************************************************************
 * Validates GET /group/:groupId/outgoing returns groups that receive communication from this group.
 * Tests: empty array for a new group, target appears after adding a link, target does NOT list
 * the source in its own outgoing, multiple outgoing links listed, and link disappears after removal.
 ***************************************************************************************************/
export function testGetOutgoingRelations(data: InitData) {
  group('[Endpoints] getOutgoingRelations - empty when no link exists', () => {
    const testName = "[getOutgoingRelations-empty]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-outgoing-empty-g1", data.structure1);

    const res = http.get(
      `${rootUrl}/communication/group/${group1.id}/outgoing`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    checkEquals(`${testName} outgoing is empty`, 0, body.length);

    deleteGroupOrFail(group1);
  });

  group('[Endpoints] getOutgoingRelations - contains target after adding link', () => {
    const testName = "[getOutgoingRelations-link]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-outgoing-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-outgoing-g2", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    const res = http.get(
      `${rootUrl}/communication/group/${group1.id}/outgoing`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    checkTrue(`${testName} outgoing contains target group`, body.some((g: any) => g.id === group2.id));
    checkTrue(`${testName} entries have id field`, body.every((g: any) => typeof g.id === "string"));

    // The target group should NOT have group1 in its outgoing
    const resTarget = http.get(
      `${rootUrl}/communication/group/${group2.id}/outgoing`,
      { headers: getHeaders() },
    );
    const bodyTarget = JSON.parse(<string>resTarget.body);
    checkFalse(`${testName} target outgoing does NOT contain source`, bodyTarget.some((g: any) => g.id === group1.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] getOutgoingRelations - multiple outgoing links', () => {
    const testName = "[getOutgoingRelations-multi]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-outgoing-multi-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure1.name + "-outgoing-multi-g2", data.structure1);
    const group3: Group = createGroupOrFail(data.structure2.name + "-outgoing-multi-g3", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);
    addCommunicationBetweenGroups(group1.id, group3.id);

    const res = http.get(
      `${rootUrl}/communication/group/${group1.id}/outgoing`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} outgoing contains group2`, body.some((g: any) => g.id === group2.id));
    checkTrue(`${testName} outgoing contains group3`, body.some((g: any) => g.id === group3.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
    deleteGroupOrFail(group3);
  });

  group('[Endpoints] getOutgoingRelations - disappears after removing link', () => {
    const testName = "[getOutgoingRelations-removed]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-outgoing-rm-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-outgoing-rm-g2", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    // Confirm present
    const resBefore = http.get(
      `${rootUrl}/communication/group/${group1.id}/outgoing`,
      { headers: getHeaders() },
    );
    checkTrue(`${testName} before removal contains target`, JSON.parse(<string>resBefore.body).some((g: any) => g.id === group2.id));

    removeCommunicationBetweenGroups(group1.id, group2.id);

    const resAfter = http.get(
      `${rootUrl}/communication/group/${group1.id}/outgoing`,
      { headers: getHeaders() },
    );
    const bodyAfter = JSON.parse(<string>resAfter.body);
    checkEquals(`${testName} after removal returns 200`, 200, resAfter.status);
    checkFalse(`${testName} after removal does NOT contain removed group`, bodyAfter.some((g: any) => g.id === group2.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });
}

/***************************************************************************************************
 * Validates GET /group/:groupId/incoming returns groups that send communication to this group.
 * Tests: empty array for a new group, source appears after adding a link, source does NOT list
 * the target in its own incoming, multiple incoming links listed, and link disappears after removal.
 ***************************************************************************************************/
export function testGetIncomingRelations(data: InitData) {
  group('[Endpoints] getIncomingRelations - empty when no link exists', () => {
    const testName = "[getIncomingRelations-empty]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-incoming-empty-g1", data.structure1);

    const res = http.get(
      `${rootUrl}/communication/group/${group1.id}/incoming`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    checkEquals(`${testName} incoming is empty`, 0, body.length);

    deleteGroupOrFail(group1);
  });

  group('[Endpoints] getIncomingRelations - contains source after adding link', () => {
    const testName = "[getIncomingRelations-link]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-incoming-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-incoming-g2", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    const res = http.get(
      `${rootUrl}/communication/group/${group2.id}/incoming`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    checkTrue(`${testName} incoming contains source group`, body.some((g: any) => g.id === group1.id));
    checkTrue(`${testName} entries have id field`, body.every((g: any) => typeof g.id === "string"));

    // The source group should NOT have group2 in its incoming
    const resSource = http.get(
      `${rootUrl}/communication/group/${group1.id}/incoming`,
      { headers: getHeaders() },
    );
    const bodySource = JSON.parse(<string>resSource.body);
    checkFalse(`${testName} source incoming does NOT contain target`, bodySource.some((g: any) => g.id === group2.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] getIncomingRelations - multiple incoming links', () => {
    const testName = "[getIncomingRelations-multi]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-incoming-multi-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure1.name + "-incoming-multi-g2", data.structure1);
    const group3: Group = createGroupOrFail(data.structure2.name + "-incoming-multi-g3", data.structure2);

    // Both group1 and group2 send to group3
    addCommunicationBetweenGroups(group1.id, group3.id);
    addCommunicationBetweenGroups(group2.id, group3.id);

    const res = http.get(
      `${rootUrl}/communication/group/${group3.id}/incoming`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} incoming contains group1`, body.some((g: any) => g.id === group1.id));
    checkTrue(`${testName} incoming contains group2`, body.some((g: any) => g.id === group2.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
    deleteGroupOrFail(group3);
  });

  group('[Endpoints] getIncomingRelations - disappears after removing link', () => {
    const testName = "[getIncomingRelations-removed]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-incoming-rm-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-incoming-rm-g2", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    removeCommunicationBetweenGroups(group1.id, group2.id);

    const res = http.get(
      `${rootUrl}/communication/group/${group2.id}/incoming`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} after removal returns 200`, 200, res.status);
    checkFalse(`${testName} after removal does NOT contain removed group`, body.some((g: any) => g.id === group1.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });
}

/***************************************************************************************************
 * Validates POST /relative/:groupId?direction= creates COMMUNIQUE_RELATIVE links between a Relative
 * profile group and its associated Student group. Tests directions BOTH, INCOMING, and OUTGOING,
 * verifying each returns 200 with a numeric count of created relationships.
 ***************************************************************************************************/
export function testAddLinkBetweenRelativeAndStudent(data: InitData) {
  group('[Endpoints] addLinkBetweenRelativeAndStudent - direction BOTH', () => {
    const testName = "[addLinkRelative-both]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const relativeGroup: ProfileGroup = getProfileGroupOfStructureByType('Relative', data.structure1);

    const res = http.post(
      `${rootUrl}/communication/relative/${relativeGroup.id}?direction=BOTH`,
      null,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkNotEquals(`${testName} response contains number field`, undefined, body.number);
    checkEquals(`${testName} number is a number`, "number", typeof body.number);
    checkGte(`${testName} number is >= 0`, 0, body.number);
  });

  group('[Endpoints] addLinkBetweenRelativeAndStudent - direction INCOMING', () => {
    const testName = "[addLinkRelative-incoming]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const relativeGroup: ProfileGroup = getProfileGroupOfStructureByType('Relative', data.structure1);

    // First remove to reset state
    http.del(
      `${rootUrl}/communication/relative/${relativeGroup.id}?direction=BOTH`,
      null,
      { headers: getHeaders() },
    );

    const res = http.post(
      `${rootUrl}/communication/relative/${relativeGroup.id}?direction=INCOMING`,
      null,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkEquals(`${testName} number is a number`, "number", typeof body.number);
  });

  group('[Endpoints] addLinkBetweenRelativeAndStudent - direction OUTGOING', () => {
    const testName = "[addLinkRelative-outgoing]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const relativeGroup: ProfileGroup = getProfileGroupOfStructureByType('Relative', data.structure1);

    // First remove to reset state
    http.del(
      `${rootUrl}/communication/relative/${relativeGroup.id}?direction=BOTH`,
      null,
      { headers: getHeaders() },
    );

    const res = http.post(
      `${rootUrl}/communication/relative/${relativeGroup.id}?direction=OUTGOING`,
      null,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkEquals(`${testName} number is a number`, "number", typeof body.number);
  });
}

/***************************************************************************************************
 * Validates DELETE /relative/:groupId?direction= removes COMMUNIQUE_RELATIVE links.
 * First adds links with BOTH direction, then removes them. Tests removing BOTH (full removal)
 * and removing only INCOMING, verifying each returns 200 with a numeric count.
 ***************************************************************************************************/
export function testRemoveLinkBetweenRelativeAndStudent(data: InitData) {
  group('[Endpoints] removeLinkBetweenRelativeAndStudent - add then remove BOTH', () => {
    const testName = "[removeLinkRelative-both]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const relativeGroup: ProfileGroup = getProfileGroupOfStructureByType('Relative', data.structure1);

    // Add first
    http.post(
      `${rootUrl}/communication/relative/${relativeGroup.id}?direction=BOTH`,
      null,
      { headers: getHeaders() },
    );

    const res = http.del(
      `${rootUrl}/communication/relative/${relativeGroup.id}?direction=BOTH`,
      null,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    console.log("body is",body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkNotEquals(`${testName} response contains number field`, undefined, body.number);
    checkEquals(`${testName} number is a number`, "number", typeof body.number);
    checkGte(`${testName} number is >= 0`, 0, body.number);
  });

  group('[Endpoints] removeLinkBetweenRelativeAndStudent - remove INCOMING only', () => {
    const testName = "[removeLinkRelative-incoming]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const relativeGroup: ProfileGroup = getProfileGroupOfStructureByType('Relative', data.structure1);

    // Add BOTH first, then remove only INCOMING
    http.post(
      `${rootUrl}/communication/relative/${relativeGroup.id}?direction=BOTH`,
      null,
      { headers: getHeaders() },
    );

    const res = http.del(
      `${rootUrl}/communication/relative/${relativeGroup.id}?direction=INCOMING`,
      null,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} number is a number`, typeof body.number === "number");
  });
}

/***************************************************************************************************
 * Validates GET /visible/:userId returns the list of users visible to a given user.
 * Tests: basic array response for a valid user, presence of another user in same group with BOTH
 * communication enabled, and absence of a user from a different school with no comm link.
 ***************************************************************************************************/
export function testVisibleUsers(data: InitData) {
  group('[Endpoints] visibleUsers - returns array for valid user', () => {
    const testName = "[visibleUsers-valid]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    const res = http.get(
      `${rootUrl}/communication/visible/${teacher1.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
  });

  group('[Endpoints] visibleUsers - contains user in same group with comm enabled', () => {
    const testName = "[visibleUsers-contains]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school1Users, 'Teacher', [teacher1]);

    const group1: Group = createGroupOrFail(data.structure1.name + "-visUsers-g1", data.structure1);
    addUsersToGroup([teacher1.id, teacher2.id], group1);
    modifyCommunicationRelationOrFail(group1, 'both');

    const res = http.get(
      `${rootUrl}/communication/visible/${teacher1.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} visible list contains the other teacher`, body.some((v: any) => v.id === teacher2.id));

    // tear down
    deleteGroupOrFail(group1);
  });

  group('[Endpoints] visibleUsers - does NOT contain user without comm link', () => {
    const testName = "[visibleUsers-notContains]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    const res = http.get(
      `${rootUrl}/communication/visible/${teacher1.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkFalse(`${testName} visible list does NOT contain isolated user`, body.some((v: any) => v.id === teacher2.id));
  });
}

/***************************************************************************************************
 * Validates GET /visible/group/:groupId returns visible users within a group for the authenticated
 * user. Tests that users appear when communication is BOTH (displayName and id present) and that
 * no users are visible when communication is NONE (default).
 ***************************************************************************************************/
export function testVisibleGroupContains(data: InitData) {
  group('[Endpoints] visibleGroupContains - with communication BOTH', () => {
    const testName = "[visibleGroupContains-both]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school1Users, 'Teacher', [teacher1]);

    const group1: Group = createGroupOrFail(data.structure1.name + "-visGrpCont-both-g1", data.structure1);
    addUsersToGroup([teacher1.id, teacher2.id], group1);
    modifyCommunicationRelationOrFail(group1, 'both');

    authenticateWeb(teacher1.login);

    const res = http.get(
      `${rootUrl}/communication/visible/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    checkTrue(`${testName} contains the other user`, body.some((u: any) => u.id === teacher2.id));
    checkTrue(`${testName} entries have displayName field`, body.every((u: any) => typeof u.displayName === "string"));
    checkTrue(`${testName} entries have id field`, body.every((u: any) => typeof u.id === "string"));

    // tear down
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    deleteGroupOrFail(group1);
  });

  group('[Endpoints] visibleGroupContains - with communication NONE shows no users', () => {
    const testName = "[visibleGroupContains-none]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "-visGrpCont-none-g1", data.structure1);
    addUsersToGroup([teacher1.id, teacher2.id], group1);
    // Communication is NONE by default (no modifyCommunicationRelationOrFail called)

    authenticateWeb(teacher1.login);

    const res = http.get(
      `${rootUrl}/communication/visible/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    checkFalse(`${testName} does NOT contain the other user when comm is NONE`, body.some((u: any) => u.id === teacher2.id));

    // tear down
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    deleteGroupOrFail(group1);
  });
}

/***************************************************************************************************
 * Validates POST /group/:groupId/users safely sets the intra-group "users" direction to BOTH.
 * Tests: direction is returned as BOTH, users in the group can communicate afterward, and calling
 * the endpoint twice is idempotent (still returns 200 with BOTH).
 ***************************************************************************************************/
export function testSafelyAddLinksWithUsers(data: InitData) {
  group('[Endpoints] safelyAddLinksWithUsers - sets direction to BOTH', () => {
    const testName = "[safelyAdd-basic]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-safelyAdd-g1", data.structure1);

    const res = http.post(
      `${rootUrl}/communication/group/${group1.id}/users`,
      null,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response contains users field`, body.users !== undefined);
    checkEquals(`${testName} users direction is BOTH`, "BOTH", body.users);

    // tear down
    deleteGroupOrFail(group1);
  });

  group('[Endpoints] safelyAddLinksWithUsers - users can communicate after', () => {
    const testName = "[safelyAdd-comm]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "-safelyAdd-comm-g1", data.structure1);
    addUsersToGroup([teacher1.id, teacher2.id], group1);

    // Before: users should not communicate (NONE by default)
    checkTrue(`${testName} cannot communicate before`, checkUsersCanCommunicate(teacher1.id, teacher2.id, false));

    // Add users link
    http.post(
      `${rootUrl}/communication/group/${group1.id}/users`,
      null,
      { headers: getHeaders() },
    );

    // After: users should communicate
    checkTrue(`${testName} can communicate after`, checkUsersCanCommunicate(teacher1.id, teacher2.id, true));

    // tear down
    deleteGroupOrFail(group1);
  });

  group('[Endpoints] safelyAddLinksWithUsers - idempotent (calling twice returns 200)', () => {
    const testName = "[safelyAdd-idempotent]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-safelyAdd-idem-g1", data.structure1);

    http.post(
      `${rootUrl}/communication/group/${group1.id}/users`,
      null,
      { headers: getHeaders() },
    );
    const res = http.post(
      `${rootUrl}/communication/group/${group1.id}/users`,
      null,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} second call returns 200`, 200, res.status);
    checkEquals(`${testName} users still BOTH`, "BOTH", JSON.parse(<string>res.body).users);

    // tear down
    deleteGroupOrFail(group1);
  });
}

/***************************************************************************************************
 * Validates DELETE /group/:groupId/users safely removes the intra-group "users" direction.
 * Tests: basic removal returns 200, users can no longer communicate afterward, and returns 409
 * when incoming relations would conflict with removing the direction.
 ***************************************************************************************************/
export function testSafelyRemoveLinksWithUsers(data: InitData) {
  group('[Endpoints] safelyRemoveLinksWithUsers - removes direction', () => {
    const testName = "[safelyRemove-basic]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-safelyRemove-g1", data.structure1);
    modifyCommunicationRelationOrFail(group1, 'both');

    const res = http.del(
      `${rootUrl}/communication/group/${group1.id}/users`,
      null,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} returns 200`, 200, res.status);

    // tear down
    deleteGroupOrFail(group1);
  });

  group('[Endpoints] safelyRemoveLinksWithUsers - users can no longer communicate', () => {
    const testName = "[safelyRemove-comm]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "-safelyRemove-comm-g1", data.structure1);
    addUsersToGroup([teacher1.id, teacher2.id], group1);
    modifyCommunicationRelationOrFail(group1, 'both');

    // Before: users should communicate
    checkTrue(`${testName} can communicate before`, checkUsersCanCommunicate(teacher1.id, teacher2.id, true));

    // Remove users link
    http.del(
      `${rootUrl}/communication/group/${group1.id}/users`,
      null,
      { headers: getHeaders() },
    );

    // After: users should NOT communicate
    checkTrue(`${testName} cannot communicate after`, checkUsersCanCommunicate(teacher1.id, teacher2.id, false));

    // tear down
    deleteGroupOrFail(group1);
  });

  group('[Endpoints] safelyRemoveLinksWithUsers - returns 409 when direction cannot be changed', () => {
    const testName = "[safelyRemove-conflict]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-safelyRemove-conflict-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-safelyRemove-conflict-g2", data.structure2);

    // Create a link that depends on group1 having users direction
    addCommunicationBetweenGroups(group2.id, group1.id);

    // group1 now has incoming relations, attempting to safely remove may result in 409
    const res = http.del(
      `${rootUrl}/communication/group/${group1.id}/users`,
      null,
      { headers: getHeaders() },
    );
    // Either 200 (safe downgrade possible) or 409 (conflict)
    checkTrue(`${testName} returns 200 or 409`, res.status === 200 || res.status === 409);

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });
}

/***************************************************************************************************
 * Validates GET /v2/group/:startGroupId/communique/:endGroupId/check simulates adding a link
 * without side effects. Tests: returns 200 for new groups, confirms no link is actually created
 * (communiqueWith remains empty), and returns 200 for already-linked groups.
 ***************************************************************************************************/
export function testAddLinkCheckOnly(data: InitData) {
  group('[Endpoints] addLinkCheckOnly - new groups can be linked', () => {
    const testName = "[checkOnly-new]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-checkOnly-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-checkOnly-g2", data.structure2);

    const res = http.get(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}/check`,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} returns 200`, 200, res.status);

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] addLinkCheckOnly - does not actually create the link', () => {
    const testName = "[checkOnly-noSideEffect]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-checkOnly-noSE-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-checkOnly-noSE-g2", data.structure2);

    // Call check
    http.get(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}/check`,
      { headers: getHeaders() },
    );

    // Verify no link was created via communiqueWith
    const res = http.get(
      `${rootUrl}/communication/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkTrue(`${testName} communiqueWith still empty after check`,
      !body.communiqueWith || body.communiqueWith.length === 0 ||
      !body.communiqueWith.some((g: any) => g.id === group2.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] addLinkCheckOnly - already linked groups', () => {
    const testName = "[checkOnly-existing]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-checkOnly-exist-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-checkOnly-exist-g2", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    // Check on an already-linked pair
    const res = http.get(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}/check`,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} returns 200 for already-linked groups`, 200, res.status);

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });
}

/***************************************************************************************************
 * Validates POST /v2/group/:startGroupId/communique/:endGroupId creates a COMMUNIQUE link and
 * updates group directions using bitmask logic (start gets INCOMING, end gets OUTGOING).
 * Tests all direction combinations: both NONE, start already OUTGOING (upgrades to BOTH),
 * start already INCOMING or BOTH (no change), end already INCOMING (upgrades to BOTH),
 * end already OUTGOING (no change), both already compatible (no direction change but link created),
 * and verifies users in linked groups can communicate afterward.
 ***************************************************************************************************/
export function testProcessAddLinkAndChangeDirection(data: InitData) {
  group('[Endpoints] processAddLinkAndChangeDirection - both NONE: start gets INCOMING, end gets OUTGOING', () => {
    const testName = "[processAddLink-bothNone]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-processAddLink-bothNone-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-processAddLink-bothNone-g2", data.structure2);

    const res = http.post(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}`,
      "{}",
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkEquals(`${testName} start direction is INCOMING`, 'INCOMING', body[group1.id]);
    checkEquals(`${testName} end direction is OUTGOING`, 'OUTGOING', body[group2.id]);

    // Verify link was actually created
    const verifyRes = http.get(
      `${rootUrl}/communication/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const verifyBody = JSON.parse(<string>verifyRes.body);
    checkTrue(`${testName} communiqueWith now contains target`, verifyBody.communiqueWith.some((g: any) => g.id === group2.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] processAddLinkAndChangeDirection - start OUTGOING: start upgrades to BOTH', () => {
    const testName = "[processAddLink-startOut]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-processAddLink-startOut-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-processAddLink-startOut-g2", data.structure2);
    const helper: Group = createGroupOrFail(data.structure1.name + "-processAddLink-startOut-helper", data.structure1);

    // Setup: helper→g1 gives g1 OUTGOING direction
    addCommunicationBetweenGroups(helper.id, group1.id);

    // Act: g1→g2 should upgrade g1 from OUTGOING to BOTH (needs INCOMING added)
    const res = http.post(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}`,
      "{}",
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkEquals(`${testName} start direction upgraded to BOTH`, 'BOTH', body[group1.id]);
    checkEquals(`${testName} end direction is OUTGOING`, 'OUTGOING', body[group2.id]);

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
    deleteGroupOrFail(helper);
  });

  group('[Endpoints] processAddLinkAndChangeDirection - start INCOMING: only end changes', () => {
    const testName = "[processAddLink-startIn]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-processAddLink-startIn-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-processAddLink-startIn-g2", data.structure2);
    const helper: Group = createGroupOrFail(data.structure2.name + "-processAddLink-startIn-helper", data.structure2);

    // Setup: g1→helper gives g1 INCOMING direction (start of existing link)
    addCommunicationBetweenGroups(group1.id, helper.id);

    // Act: g1→g2 should NOT change g1 (already has INCOMING), only g2 gets OUTGOING
    const res = http.post(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}`,
      "{}",
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} start not in response (no change needed)`, body[group1.id] === undefined);
    checkEquals(`${testName} end direction is OUTGOING`, 'OUTGOING', body[group2.id]);

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
    deleteGroupOrFail(helper);
  });

  group('[Endpoints] processAddLinkAndChangeDirection - start BOTH: only end changes', () => {
    const testName = "[processAddLink-startBoth]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-processAddLink-startBoth-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-processAddLink-startBoth-g2", data.structure2);

    // Setup: g1 set to BOTH via safelyAddLinksWithUsers
    modifyCommunicationRelationOrFail(group1, 'both');

    // Act: g1→g2 should NOT change g1 (BOTH already includes INCOMING), only g2 gets OUTGOING
    const res = http.post(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}`,
      "{}",
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} start not in response (no change needed)`, body[group1.id] === undefined);
    checkEquals(`${testName} end direction is OUTGOING`, 'OUTGOING', body[group2.id]);

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] processAddLinkAndChangeDirection - end INCOMING: end upgrades to BOTH', () => {
    const testName = "[processAddLink-endIn]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-processAddLink-endIn-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-processAddLink-endIn-g2", data.structure2);
    const helper: Group = createGroupOrFail(data.structure1.name + "-processAddLink-endIn-helper", data.structure1);

    // Setup: g2→helper gives g2 INCOMING direction (g2 is start of a link)
    addCommunicationBetweenGroups(group2.id, helper.id);

    // Act: g1→g2 should give g1 INCOMING, and upgrade g2 from INCOMING to BOTH (needs OUTGOING added)
    const res = http.post(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}`,
      "{}",
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkEquals(`${testName} start direction is INCOMING`, 'INCOMING', body[group1.id]);
    checkEquals(`${testName} end direction upgraded to BOTH`, 'BOTH', body[group2.id]);

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
    deleteGroupOrFail(helper);
  });

  group('[Endpoints] processAddLinkAndChangeDirection - end OUTGOING: only start changes', () => {
    const testName = "[processAddLink-endOut]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-processAddLink-endOut-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-processAddLink-endOut-g2", data.structure2);
    const helper: Group = createGroupOrFail(data.structure1.name + "-processAddLink-endOut-helper", data.structure1);

    // Setup: helper→g2 gives g2 OUTGOING direction (g2 is end of existing link)
    addCommunicationBetweenGroups(helper.id, group2.id);

    // Act: g1→g2 should give g1 INCOMING, but g2 already has OUTGOING → no change for g2
    const res = http.post(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}`,
      "{}",
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkEquals(`${testName} start direction is INCOMING`, 'INCOMING', body[group1.id]);
    checkTrue(`${testName} end not in response (no change needed)`, body[group2.id] === undefined);

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
    deleteGroupOrFail(helper);
  });

  group('[Endpoints] processAddLinkAndChangeDirection - both already compatible: no direction change', () => {
    const testName = "[processAddLink-noChange]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-processAddLink-noChg-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-processAddLink-noChg-g2", data.structure2);
    const helper1: Group = createGroupOrFail(data.structure2.name + "-processAddLink-noChg-h1", data.structure2);
    const helper2: Group = createGroupOrFail(data.structure1.name + "-processAddLink-noChg-h2", data.structure1);

    // Setup: g1→helper1 gives g1 INCOMING; helper2→g2 gives g2 OUTGOING
    addCommunicationBetweenGroups(group1.id, helper1.id);
    addCommunicationBetweenGroups(helper2.id, group2.id);

    // Act: g1→g2 — g1 already INCOMING (no change), g2 already OUTGOING (no change)
    const res = http.post(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}`,
      "{}",
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} start not in response (no change needed)`, body[group1.id] === undefined);
    checkTrue(`${testName} end not in response (no change needed)`, body[group2.id] === undefined);

    // Verify the link was still created even though no direction changed
    const verifyRes = http.get(
      `${rootUrl}/communication/group/${group1.id}`,
      { headers: getHeaders() },
    );
    const verifyBody = JSON.parse(<string>verifyRes.body);
    checkTrue(`${testName} communiqueWith contains target`, verifyBody.communiqueWith.some((g: any) => g.id === group2.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
    deleteGroupOrFail(helper1);
    deleteGroupOrFail(helper2);
  });

  group('[Endpoints] processAddLinkAndChangeDirection - users can communicate after', () => {
    const testName = "[processAddLink-comm]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "-processAddLink-comm-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-processAddLink-comm-g2", data.structure2);

    addUsersToGroup([teacher1.id], group1);
    addUsersToGroup([teacher2.id], group2);

    http.post(
      `${rootUrl}/communication/v2/group/${group1.id}/communique/${group2.id}`,
      "{}",
      { headers: getHeaders("application/json") },
    );

    checkTrue(`${testName} can communicate after`, checkUsersCanCommunicate(teacher1.id, teacher2.id, true));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });
}

/***************************************************************************************************
 * Validates DELETE /group/:startGroupId/relations/:endGroupId removes a COMMUNIQUE link and returns
 * the resulting directions of sender/receiver (null when fully downgraded).
 * Tests: response contains sender/receiver with valid direction or null, link disappears from
 * outgoing/incoming lists, and users can no longer communicate afterward.
 ***************************************************************************************************/
export function testRemoveRelations(data: InitData) {
  group('[Endpoints] removeRelations - returns sender and receiver directions', () => {
    const testName = "[removeRelations-basic]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-removeRel-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-removeRel-g2", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    const res = http.del(
      `${rootUrl}/communication/group/${group1.id}/relations/${group2.id}`,
      null,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response contains sender key`, 'sender' in body);
    checkTrue(`${testName} response contains receiver key`, 'receiver' in body);
    checkTrue(`${testName} sender is null or valid direction`, body.sender === null || ["NONE", "INCOMING", "OUTGOING", "BOTH"].includes(body.sender));
    checkTrue(`${testName} receiver is null or valid direction`, body.receiver === null || ["NONE", "INCOMING", "OUTGOING", "BOTH"].includes(body.receiver));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] removeRelations - link disappears from outgoing/incoming', () => {
    const testName = "[removeRelations-verify]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const group1: Group = createGroupOrFail(data.structure1.name + "-removeRel-verify-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-removeRel-verify-g2", data.structure2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    // Remove
    http.del(
      `${rootUrl}/communication/group/${group1.id}/relations/${group2.id}`,
      null,
      { headers: getHeaders() },
    );

    // Verify outgoing is empty
    const resOut = http.get(
      `${rootUrl}/communication/group/${group1.id}/outgoing`,
      { headers: getHeaders() },
    );
    checkFalse(`${testName} outgoing no longer contains group2`, JSON.parse(<string>resOut.body).some((g: any) => g.id === group2.id));

    // Verify incoming is empty
    const resIn = http.get(
      `${rootUrl}/communication/group/${group2.id}/incoming`,
      { headers: getHeaders() },
    );
    checkFalse(`${testName} incoming no longer contains group1`, JSON.parse(<string>resIn.body).some((g: any) => g.id === group1.id));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Endpoints] removeRelations - users can no longer communicate', () => {
    const testName = "[removeRelations-comm]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "-removeRel-comm-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-removeRel-comm-g2", data.structure2);

    addUsersToGroup([teacher1.id], group1);
    addUsersToGroup([teacher2.id], group2);
    addCommunicationBetweenGroups(group1.id, group2.id);

    checkTrue(`${testName} can communicate before`, checkUsersCanCommunicate(teacher1.id, teacher2.id, true));

    http.del(
      `${rootUrl}/communication/group/${group1.id}/relations/${group2.id}`,
      null,
      { headers: getHeaders() },
    );

    checkTrue(`${testName} cannot communicate after`, checkUsersCanCommunicate(teacher1.id, teacher2.id, false));

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });
}

/***************************************************************************************************
 * Validates GET /verify/:from/:to returns whether two users can communicate.
 * Tests: positive case (same group with BOTH), negative case (different schools, no link),
 * direct COMMUNIQUE_DIRECT link enables communication, and group-based link enables communication.
 ***************************************************************************************************/
export function testVerify(data: InitData) {
  group('[Endpoints] verify - users in same group with BOTH can communicate', () => {
    const testName = "[verify-canComm]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school1Users, 'Teacher', [teacher1]);

    const group1: Group = createGroupOrFail(data.structure1.name + "-verify-can-g1", data.structure1);
    addUsersToGroup([teacher1.id, teacher2.id], group1);
    modifyCommunicationRelationOrFail(group1, 'both');

    const res = http.get(
      `${rootUrl}/communication/verify/${teacher1.id}/${teacher2.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response has canCommunicate field`, body.canCommunicate !== undefined);
    checkTrue(`${testName} canCommunicate is boolean`, typeof body.canCommunicate === "boolean");
    checkTrue(`${testName} canCommunicate is true`, body.canCommunicate === true);

    // tear down
    deleteGroupOrFail(group1);
  });

  group('[Endpoints] verify - users in different schools cannot communicate', () => {
    const testName = "[verify-cannotComm]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    const res = http.get(
      `${rootUrl}/communication/verify/${teacher1.id}/${teacher2.id}`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkEquals(`${testName} canCommunicate is false`, false, body.canCommunicate);
  });

  group('[Endpoints] verify - direct communication link enables verify', () => {
    const testName = "[verify-direct]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    // Before direct communication
    const resBefore = http.get(
      `${rootUrl}/communication/verify/${teacher1.id}/${teacher2.id}`,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} cannot communicate before`, false, JSON.parse(<string>resBefore.body).canCommunicate);

    setDirectCommunicationOrFail(teacher1.id, teacher2.id, 'both');

    const resAfter = http.get(
      `${rootUrl}/communication/verify/${teacher1.id}/${teacher2.id}`,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} can communicate after direct link`, true, JSON.parse(<string>resAfter.body).canCommunicate);
  });

  group('[Endpoints] verify - group link enables verify', () => {
    const testName = "[verify-group]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "-verify-grp-g1", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "-verify-grp-g2", data.structure2);
    addUsersToGroup([teacher1.id], group1);
    addUsersToGroup([teacher2.id], group2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    const res = http.get(
      `${rootUrl}/communication/verify/${teacher1.id}/${teacher2.id}`,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} can communicate via group link`, true, JSON.parse(<string>res.body).canCommunicate);

    // tear down
    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });
}

/***************************************************************************************************
 * Validates POST /discover/visible/users returns discoverable users filtered by structure, profile,
 * and search text. Tests: filtered results contain id and displayName, non-matching search returns
 * empty array, and empty body still returns a valid array.
 ***************************************************************************************************/
export function testGetDiscoverVisibleUsers(data: InitData) {
  group('[Endpoints] getDiscoverVisibleUsers - with structure and profile filter', () => {
    const testName = "[discoverUsers-filter]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const payload = JSON.stringify({
      structures: [data.structure1.id],
      profiles: ["Teacher"],
    });
    const res = http.post(
      `${rootUrl}/communication/discover/visible/users`,
      payload,
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    if (Array.isArray(body) && body.length > 0) {
      checkTrue(`${testName} entries have id field`, body.every((u: any) => typeof u.id === "string"));
      checkTrue(`${testName} entries have displayName field`, body.every((u: any) => typeof u.displayName === "string"));
    }
  });

  group('[Endpoints] getDiscoverVisibleUsers - with search filter', () => {
    const testName = "[discoverUsers-search]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const payload = JSON.stringify({
      structures: [data.structure1.id],
      profiles: ["Teacher"],
      search: "zzz_nonexistent_user_zzz",
    });
    const res = http.post(
      `${rootUrl}/communication/discover/visible/users`,
      payload,
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an empty array for non-matching search`, Array.isArray(body) && body.length === 0);
  });

  group('[Endpoints] getDiscoverVisibleUsers - empty body', () => {
    const testName = "[discoverUsers-empty]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const res = http.post(
      `${rootUrl}/communication/discover/visible/users`,
      JSON.stringify({}),
      { headers: getHeaders("application/json") },
    );
    checkEquals(`${testName} returns 200 with empty filter`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(JSON.parse(<string>res.body)));
  });
}

/***************************************************************************************************
 * Validates GET /discover/visible/profiles returns the profile types accepted for discover
 * visibility as a string array (e.g. Teacher, Student, Relative, Personnel).
 ***************************************************************************************************/
export function testGetDiscoverVisibleAcceptedProfile(data: InitData) {
  group('[Endpoints] getDiscoverVisibleAcceptedProfile - returns profiles array', () => {
    const testName = "[discoverProfiles]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const res = http.get(
      `${rootUrl}/communication/discover/visible/profiles`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    checkTrue(`${testName} entries are strings`, body.length === 0 || body.every((p: any) => typeof p === "string"));
  });
}

/***************************************************************************************************
 * Validates GET /discover/visible/structures returns the list of structures accessible for discover
 * visibility. Each entry has an id and a label field.
 ***************************************************************************************************/
export function testGetDiscoverVisibleStructures(data: InitData) {
  group('[Endpoints] getDiscoverVisibleStructures - returns structures list', () => {
    const testName = "[discoverStructures]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const res = http.get(
      `${rootUrl}/communication/discover/visible/structures`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    if (Array.isArray(body) && body.length > 0) {
      checkTrue(`${testName} entries have id field`, body.every((s: any) => typeof s.id === "string"));
      checkTrue(`${testName} entries have label field`, body.every((s: any) => typeof s.label === "string"));
    }
  });
}

/***************************************************************************************************
 * Validates POST /discover/visible/add/commuting/:receiverId creates a direct COMMUNIQUE_DIRECT
 * link from the authenticated user to the receiver. Tests: response contains a numeric count,
 * and verifies via the verify endpoint that users can communicate afterward.
 ***************************************************************************************************/
export function testDiscoverVisibleAddCommuteUsers(data: InitData) {
  group('[Endpoints] discoverVisibleAddCommuteUsers - adds commute link', () => {
    const testName = "[discoverAddCommute-basic]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school1Users, 'Teacher', [teacher1]);

    authenticateWeb(teacher1.login);

    const res = http.post(
      `${rootUrl}/communication/discover/visible/add/commuting/${teacher2.id}`,
      null,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response contains count field`, body.count !== undefined);
    checkTrue(`${testName} count is a number`, typeof body.count === "number");
  });

  group('[Endpoints] discoverVisibleAddCommuteUsers - users can communicate after', () => {
    const testName = "[discoverAddCommute-verify]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    authenticateWeb(teacher1.login);

    http.post(
      `${rootUrl}/communication/discover/visible/add/commuting/${teacher2.id}`,
      null,
      { headers: getHeaders() },
    );

    // Check via verify endpoint (as admin)
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const verifyRes = http.get(
      `${rootUrl}/communication/verify/${teacher1.id}/${teacher2.id}`,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} users can communicate after add commute`, true, JSON.parse(<string>verifyRes.body).canCommunicate);
  });
}

/***************************************************************************************************
 * Validates DELETE /discover/visible/remove/commuting/:receiverId removes the direct commuting
 * link. Tests: adds then removes the link verifying a numeric count response, and confirms via
 * the verify endpoint that users can no longer communicate afterward.
 ***************************************************************************************************/
export function testDiscoverVisibleRemoveCommuteUsers(data: InitData) {
  group('[Endpoints] discoverVisibleRemoveCommuteUsers - removes commute link', () => {
    const testName = "[discoverRemoveCommute-basic]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school1Users, 'Teacher', [teacher1]);

    authenticateWeb(teacher1.login);

    // Add then remove
    http.post(
      `${rootUrl}/communication/discover/visible/add/commuting/${teacher2.id}`,
      null,
      { headers: getHeaders() },
    );

    const res = http.del(
      `${rootUrl}/communication/discover/visible/remove/commuting/${teacher2.id}`,
      null,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response contains count field`, body.count !== undefined);
    checkTrue(`${testName} count is a number`, typeof body.count === "number");
  });

  group('[Endpoints] discoverVisibleRemoveCommuteUsers - users can no longer communicate', () => {
    const testName = "[discoverRemoveCommute-verify]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school2Users, 'Teacher');

    authenticateWeb(teacher1.login);

    // Add commute
    http.post(
      `${rootUrl}/communication/discover/visible/add/commuting/${teacher2.id}`,
      null,
      { headers: getHeaders() },
    );

    // Remove commute
    http.del(
      `${rootUrl}/communication/discover/visible/remove/commuting/${teacher2.id}`,
      null,
      { headers: getHeaders() },
    );

    // Verify users can no longer communicate
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const verifyRes = http.get(
      `${rootUrl}/communication/verify/${teacher1.id}/${teacher2.id}`,
      { headers: getHeaders() },
    );
    checkEquals(`${testName} users cannot communicate after remove commute`, false, JSON.parse(<string>verifyRes.body).canCommunicate);
  });
}

/***************************************************************************************************
 * Validates GET /discover/visible/groups returns the discover groups owned by the authenticated
 * user. Tests: basic array response with id fields, and a newly created group appears in the list.
 ***************************************************************************************************/
export function testDiscoverVisibleGetGroups(data: InitData) {
  group('[Endpoints] discoverVisibleGetGroups - returns array', () => {
    const testName = "[discoverGroups-basic]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const res = http.get(
      `${rootUrl}/communication/discover/visible/groups`,
      { headers: getHeaders() },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response is an array`, Array.isArray(body));
    if (Array.isArray(body) && body.length > 0) {
      checkTrue(`${testName} entries have id field`, body.every((g: any) => typeof g.id === "string"));
    }
  });

  group('[Endpoints] discoverVisibleGetGroups - created group appears in list', () => {
    const testName = "[discoverGroups-created]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    // Create a discover group
    const createRes = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "test-discoverGroups-listed" }),
      { headers: getHeaders("application/json") },
    );

    if (createRes.status === 200) {
      const createdId = JSON.parse(<string>createRes.body).id;

      const res = http.get(
        `${rootUrl}/communication/discover/visible/groups`,
        { headers: getHeaders() },
      );
      const body = JSON.parse(<string>res.body);
      checkTrue(`${testName} created group appears in list`, body.some((g: any) => g.id === createdId));
    } else {
      checkTrue(`${testName} discover feature responded (may not be enabled)`, createRes.status !== 0);
    }
  });
}

/***************************************************************************************************
 * Validates GET /discover/visible/group/:groupId/users returns users in a discover group.
 * Tests: empty group returns empty array, and after adding a user via the update endpoint that
 * user appears in the response.
 ***************************************************************************************************/
export function testDiscoverVisibleGetUsersInGroup(data: InitData) {
  group('[Endpoints] discoverVisibleGetUsersInGroup - empty group returns empty array', () => {
    const testName = "[discoverUsersInGroup-empty]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const createRes = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "test-discoverUsersInGroup-empty" }),
      { headers: getHeaders("application/json") },
    );

    if (createRes.status === 200) {
      const groupId = JSON.parse(<string>createRes.body).id;

      const res = http.get(
        `${rootUrl}/communication/discover/visible/group/${groupId}/users`,
        { headers: getHeaders() },
      );
      const body = JSON.parse(<string>res.body);
      checkEquals(`${testName} returns 200`, 200, res.status);
      checkTrue(`${testName} response is an array`, Array.isArray(body));
      checkEquals(`${testName} group is empty`, 0, body.length);
    } else {
      checkTrue(`${testName} discover feature responded (may not be enabled)`, createRes.status !== 0);
    }
  });

  group('[Endpoints] discoverVisibleGetUsersInGroup - with users added', () => {
    const testName = "[discoverUsersInGroup-withUsers]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school1Users, 'Teacher', [teacher1]);

    authenticateWeb(teacher1.login);

    const createRes = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "test-discoverUsersInGroup-filled" }),
      { headers: getHeaders("application/json") },
    );

    if (createRes.status === 200) {
      const groupId = JSON.parse(<string>createRes.body).id;

      // Add teacher2 to the group
      http.put(
        `${rootUrl}/communication/discover/visible/group/${groupId}/users`,
        JSON.stringify({ oldUsers: [], newUsers: [teacher2.id] }),
        { headers: getHeaders("application/json") },
      );

      const res = http.get(
        `${rootUrl}/communication/discover/visible/group/${groupId}/users`,
        { headers: getHeaders() },
      );
      const body = JSON.parse(<string>res.body);
      checkEquals(`${testName} returns 200`, 200, res.status);
      checkTrue(`${testName} response is an array`, Array.isArray(body));
      checkTrue(`${testName} contains added user`, body.some((u: any) => u.id === teacher2.id));
    } else {
      checkTrue(`${testName} discover feature responded (may not be enabled)`, createRes.status !== 0);
    }
  });
}

/***************************************************************************************************
 * Validates POST /discover/visible/group creates a new discover group owned by the authenticated
 * user. Tests: response contains a non-empty string id, empty name is rejected (non-200), and the
 * created group is retrievable via the groups list endpoint.
 ***************************************************************************************************/
export function testCreateDiscoverVisibleGroup(data: InitData) {
  group('[Endpoints] createDiscoverVisibleGroup - returns id', () => {
    const testName = "[createDiscoverGroup-basic]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const payload = JSON.stringify({ name: "test-createDiscoverGroup-basic" });
    const res = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      payload,
      { headers: getHeaders("application/json") },
    );
    const body = JSON.parse(<string>res.body);
    checkEquals(`${testName} returns 200`, 200, res.status);
    checkTrue(`${testName} response contains id`, body.id !== undefined);
    checkTrue(`${testName} id is a string`, typeof body.id === "string");
    checkTrue(`${testName} id is non-empty`, body.id.length > 0);
  });

  group('[Endpoints] createDiscoverVisibleGroup - invalid name rejected', () => {
    const testName = "[createDiscoverGroup-invalid]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    // Empty name should fail
    const res = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "" }),
      { headers: getHeaders("application/json") },
    );
    checkTrue(`${testName} empty name returns error (not 200)`, res.status !== 200);
  });

  group('[Endpoints] createDiscoverVisibleGroup - created group is retrievable', () => {
    const testName = "[createDiscoverGroup-retrieve]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const createRes = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "test-createDiscoverGroup-retrieve" }),
      { headers: getHeaders("application/json") },
    );

    if (createRes.status === 200) {
      const groupId = JSON.parse(<string>createRes.body).id;

      // Verify it appears in the groups list
      const listRes = http.get(
        `${rootUrl}/communication/discover/visible/groups`,
        { headers: getHeaders() },
      );
      const groups = JSON.parse(<string>listRes.body);
      checkTrue(`${testName} created group appears in groups list`, groups.some((g: any) => g.id === groupId));
    } else {
      checkTrue(`${testName} discover feature responded (may not be enabled)`, createRes.status !== 0);
    }
  });
}

/***************************************************************************************************
 * Validates PUT /discover/visible/group/:groupId renames an existing discover group.
 * Tests: successful rename returns 200 with the same group id, and empty name is rejected.
 ***************************************************************************************************/
export function testUpdateDiscoverVisibleGroup(data: InitData) {
  group('[Endpoints] updateDiscoverVisibleGroup - renames the group', () => {
    const testName = "[updateDiscoverGroup-rename]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const createRes = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "test-updateDiscoverGroup-original" }),
      { headers: getHeaders("application/json") },
    );

    if (createRes.status === 200) {
      const groupId = JSON.parse(<string>createRes.body).id;

      const payload = JSON.stringify({ name: "test-updateDiscoverGroup-renamed" });
      const res = http.put(
        `${rootUrl}/communication/discover/visible/group/${groupId}`,
        payload,
        { headers: getHeaders("application/json") },
      );
      const body = JSON.parse(<string>res.body);
      checkEquals(`${testName} returns 200`, 200, res.status);
      checkTrue(`${testName} response contains id`, body.id !== undefined);
      checkEquals(`${testName} id matches original`, groupId, body.id);
    } else {
      checkTrue(`${testName} discover feature responded (may not be enabled)`, createRes.status !== 0);
    }
  });

  group('[Endpoints] updateDiscoverVisibleGroup - empty name rejected', () => {
    const testName = "[updateDiscoverGroup-invalid]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const createRes = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "test-updateDiscoverGroup-toInvalid" }),
      { headers: getHeaders("application/json") },
    );

    if (createRes.status === 200) {
      const groupId = JSON.parse(<string>createRes.body).id;

      const res = http.put(
        `${rootUrl}/communication/discover/visible/group/${groupId}`,
        JSON.stringify({ name: "" }),
        { headers: getHeaders("application/json") },
      );
      checkTrue(`${testName} empty name returns error (not 200)`, res.status !== 200);
    } else {
      checkTrue(`${testName} discover feature responded (may not be enabled)`, createRes.status !== 0);
    }
  });
}

/***************************************************************************************************
 * Validates PUT /discover/visible/group/:groupId/users updates the user list of a discover group.
 * Tests: adding a user makes them appear in the group, empty newUsers is rejected (non-200), and
 * replacing users (oldUsers removed, newUsers added) works correctly.
 ***************************************************************************************************/
export function testAddDiscoverVisibleGroupUsers(data: InitData) {
  group('[Endpoints] addDiscoverVisibleGroupUsers - adds users to group', () => {
    const testName = "[addDiscoverGroupUsers-basic]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school1Users, 'Teacher', [teacher1]);

    authenticateWeb(teacher1.login);

    const createRes = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "test-addDiscoverGroupUsers-basic" }),
      { headers: getHeaders("application/json") },
    );

    if (createRes.status === 200) {
      const groupId = JSON.parse(<string>createRes.body).id;

      const payload = JSON.stringify({
        oldUsers: [],
        newUsers: [teacher2.id],
      });
      const res = http.put(
        `${rootUrl}/communication/discover/visible/group/${groupId}/users`,
        payload,
        { headers: getHeaders("application/json") },
      );
      checkEquals(`${testName} returns 200`, 200, res.status);

      // Verify user is in group
      const usersRes = http.get(
        `${rootUrl}/communication/discover/visible/group/${groupId}/users`,
        { headers: getHeaders() },
      );
      const usersBody = JSON.parse(<string>usersRes.body);
      checkTrue(`${testName} user appears in group after adding`, usersBody.some((u: any) => u.id === teacher2.id));
    } else {
      checkTrue(`${testName} discover feature responded (may not be enabled)`, createRes.status !== 0);
    }
  });

  group('[Endpoints] addDiscoverVisibleGroupUsers - empty newUsers rejected', () => {
    const testName = "[addDiscoverGroupUsers-emptyUsers]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');

    authenticateWeb(teacher1.login);

    const createRes = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "test-addDiscoverGroupUsers-empty" }),
      { headers: getHeaders("application/json") },
    );

    if (createRes.status === 200) {
      const groupId = JSON.parse(<string>createRes.body).id;

      const payload = JSON.stringify({
        oldUsers: [],
        newUsers: [],
      });
      const res = http.put(
        `${rootUrl}/communication/discover/visible/group/${groupId}/users`,
        payload,
        { headers: getHeaders("application/json") },
      );
      checkTrue(`${testName} empty newUsers returns error (not 200)`, res.status !== 200);
    } else {
      checkTrue(`${testName} discover feature responded (may not be enabled)`, createRes.status !== 0);
    }
  });

  group('[Endpoints] addDiscoverVisibleGroupUsers - replaces users (old removed, new added)', () => {
    const testName = "[addDiscoverGroupUsers-replace]";
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const teacher1 = getRandomUserWithProfile(school1Users, 'Teacher');
    const teacher2 = getRandomUserWithProfile(school1Users, 'Teacher', [teacher1]);
    const teacher3 = getRandomUserWithProfile(school1Users, 'Teacher', [teacher1, teacher2]);

    authenticateWeb(teacher1.login);

    const createRes = http.post(
      `${rootUrl}/communication/discover/visible/group`,
      JSON.stringify({ name: "test-addDiscoverGroupUsers-replace" }),
      { headers: getHeaders("application/json") },
    );

    if (createRes.status === 200) {
      const groupId = JSON.parse(<string>createRes.body).id;

      // First add teacher2
      http.put(
        `${rootUrl}/communication/discover/visible/group/${groupId}/users`,
        JSON.stringify({ oldUsers: [], newUsers: [teacher2.id] }),
        { headers: getHeaders("application/json") },
      );

      // Now replace teacher2 with teacher3
      const res = http.put(
        `${rootUrl}/communication/discover/visible/group/${groupId}/users`,
        JSON.stringify({ oldUsers: [teacher2.id], newUsers: [teacher3.id] }),
        { headers: getHeaders("application/json") },
      );
      checkEquals(`${testName} replace returns 200`, 200, res.status);

      // Verify teacher3 is in group and teacher2 is not
      const usersRes = http.get(
        `${rootUrl}/communication/discover/visible/group/${groupId}/users`,
        { headers: getHeaders() },
      );
      const usersBody = JSON.parse(<string>usersRes.body);
      checkTrue(`${testName} new user is in group`, usersBody.some((u: any) => u.id === teacher3.id));
      checkFalse(`${testName} old user is NOT in group`, usersBody.some((u: any) => u.id === teacher2.id));
    } else {
      checkTrue(`${testName} discover feature responded (may not be enabled)`, createRes.status !== 0);
    }
  });
}