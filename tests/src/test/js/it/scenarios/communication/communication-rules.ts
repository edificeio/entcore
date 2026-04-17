import {
  authenticateWeb,
  initStructure,
  Session,
  Structure,
  getUsersOfSchool,
  getRandomUserWithProfile,
  addCommunicationBetweenGroups,
  createGroupOrFail,
  Group,
  addUsersToGroup,
  modifyCommunicationRelationOrFail,
  addCommunicationRelationOrFail,
  removeCommunicationOrFail,
  safelyRemoveCommunicationFromBothOrFail,
  deleteGroupOrFail,
  searchVisiblesOrFail,
  Visible,
  getProfileGroupOfStructure,
  setDirectCommunicationOrFail,
  safelyModifyCommunicationToBothOrFail
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import {check, group} from "k6";


const maxDuration = __ENV.MAX_DURATION || "20m";
const schoolName = __ENV.DATA_SCHOOL_NAME || "Test communication rules";
const gracefulStop = parseInt(__ENV.GRACEFUL_STOP || "2s");

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    testGroupCommunication: {
      executor: "per-vu-iterations",
      exec: "testGroupCommunications",
      vus: 1,
      maxDuration: maxDuration,
      gracefulStop,
    },
  },
};

type InitData = {
  structure1: Structure;
  structure2: Structure;
}

export function setup() {
  let structure1: Structure;
  let structure2: Structure;

  group("[Communications-Rules] Initialize data", () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure1 = initStructure(`${schoolName} - School1`);
    structure2 = initStructure(`${schoolName} - School2`);
  });
  return { structure1 : structure1, structure2 : structure2 };
}

export function testGroupCommunications(data: InitData){
  /*******************************************************************************************************
   *  Communication in the Group Ua -> G -> Ub
   ******************************************************************************************************/

  group('[Communication] Test that we can communicate A => G => B when communication is BOTH for G ', () => {
    const testName = "[ A => G => B when communication is BOTH for G ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "group1-both", data.structure1);

    addUsersToGroup([school1Teacher.id, school2Teacher.id], group1);
    modifyCommunicationRelationOrFail(group1, 'both');

    console.log(`Group created ${group1.name}, id ${group1.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    deleteGroupOrFail(group1);
  });

  group('[Communication] Test that we can\'t communicate A => G => B when communication is NONE for G ', () => {
    const testName = "[  A => G => B when communication is NONE for G ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "group1-none", data.structure1);

    addUsersToGroup([school1Teacher.id, school2Teacher.id], group1);

    console.log(`Group created ${group1.name}, id ${group1.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, {[`${testName} Visible should not contain the second user`] : visibles => !visibles.find( v => v.id === school2Teacher.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    deleteGroupOrFail(group1);
  });

  group('[Communication] Test that we can\'t communicate A => G => B when communication is INCOMING for G ', () => {
    const testName = "[  A => G => B when communication is INCOMING for G ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "group1-incoming", data.structure1);

    addUsersToGroup([school1Teacher.id, school2Teacher.id], group1);
    modifyCommunicationRelationOrFail(group1, 'incoming');

    console.log(`Group created ${group1.name}, id ${group1.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => !visibles.find( v => v.id === school2Teacher.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    deleteGroupOrFail(group1);
  });

  group('[Communication] Test that we can\'t communicate A => G => B when communication is OUTGOING for G ', () => {
    const testName = "[  A => G => B when communication is OUTGOING for G ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "group1-outgoing", data.structure1);

    addUsersToGroup([school1Teacher.id, school2Teacher.id], group1);
    modifyCommunicationRelationOrFail(group1, 'outgoing');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`login ${school1Teacher.login}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );

    console.log( visibles.find( v => v.id === school2Teacher.id ));

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    deleteGroupOrFail(group1);
  });

  /*******************************************************************************************************
   *  Communication inter Group Ua -> G1 -> G2 -> Ub G2 = Outgoing
   ******************************************************************************************************/

  group('[Communication] Test that we can\'t communicate A => G1 => G2 => B when communication is OUTGOING for G1 and G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is OUTGOING for G1 and G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-outgoings", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-outgoings", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group1);
    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    removeCommunicationOrFail(group1, 'both');
    modifyCommunicationRelationOrFail(group1, 'outgoing');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should not contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });


  group('[Communication] Test that we can communicate A => G1 => G2 => B when communication is INCOMING for G1 and OUTGOING G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is INCOMING for G1 and OUTGOING G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-incoming", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-outgoing", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group1);
    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, {[`${testName} Visible should contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) !== null } );
    check(visibles, { [`${testName} Visible should contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can communicate A => G1 => G2 => B when communication is BOTH for G1 and OUTGOING G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is BOTH for G1 and OUTGOING G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-both", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-outgoing", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group1.id, group2.id);
    safelyModifyCommunicationToBothOrFail(group1);

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, {[`${testName} Visible should contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) !== null } );
    check(visibles, { [`${testName} Visible should contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can\'t communicate A => G1 => G2 => B when communication is NONE for G1 and OUTGOING G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is NONE for G1 and OUTGOING G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-none", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-outgoing", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group1);
    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    removeCommunicationOrFail(group1, 'both');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should not contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });



  /*******************************************************************************************************
   *  Communication inter Group Ua -> G1 -> G2 -> Ub G2 = INCOMING
   ******************************************************************************************************/

  group('[Communication] Test that we can\'t communicate A => G1 => G2 => B when communication is OUTGOING for G1 and INCOMING G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is OUTGOING for G1 and INCOMING G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-outgoings", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-incoming", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group1);
    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    removeCommunicationOrFail(group1, 'both');
    removeCommunicationOrFail(group2, 'both');

    modifyCommunicationRelationOrFail(group1, 'outgoing');
    modifyCommunicationRelationOrFail(group2, 'incoming');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should not contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can communicate A => G1 => G2 when communication is INCOMING for G1 and INCOMING G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is INCOMING for G1 and INCOMING G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-incoming", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-incoming", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group1);
    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    removeCommunicationOrFail(group2, 'both');
    modifyCommunicationRelationOrFail(group2, 'incoming');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can communicate A => G1 => G2 when communication is BOTH for G1 and INCOMING G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is BOTH for G1 and INCOMING G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-both", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-incoming", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group1.id, group2.id);

    safelyModifyCommunicationToBothOrFail(group1);
    removeCommunicationOrFail(group2, 'both');
    modifyCommunicationRelationOrFail(group2, 'incoming');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can\'t communicate A => G1 => G2 => B when communication is NONE for G1 and INCOMING G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is NONE for G1 and INCOMING G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-none", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-incoming", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);
    safelyRemoveCommunicationFromBothOrFail(group1);

    addCommunicationBetweenGroups(group1.id, group2.id);

    modifyCommunicationRelationOrFail(group2, 'incoming');
    removeCommunicationOrFail(group1, 'both');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should not contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });


  /*******************************************************************************************************
   *  Communication inter Group Ua -> G1 -> G2 -> Ub G2 = BOTH
   ******************************************************************************************************/

  group('[Communication] Test that we can\'t communicate A => G1 => G2 => B when communication is OUTGOING for G1 and BOTH G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is OUTGOING for G1 and BOTH G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-outgoing", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-both", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group1);

    addCommunicationBetweenGroups(group1.id, group2.id);

    safelyModifyCommunicationToBothOrFail(group2);
    removeCommunicationOrFail(group1, 'both');
    modifyCommunicationRelationOrFail(group1, 'outgoing');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should not contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });


  group('[Communication] Test that we can communicate A => G1 => G2 => B when communication is INCOMING for G1 and BOTH G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is INCOMING for G1 and BOTH G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-incoming", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-both", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);
    safelyRemoveCommunicationFromBothOrFail(group1);
    addCommunicationBetweenGroups(group1.id, group2.id);

    safelyModifyCommunicationToBothOrFail(group2);

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, {[`${testName} Visible should contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) !== null } );
    check(visibles, { [`${testName} Visible should contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can communicate A => G1 => G2 when communication is BOTH for G1 and BOTH G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is BOTH for G1 and BOTH G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-both", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-both", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);
    safelyRemoveCommunicationFromBothOrFail(group1);
    addCommunicationBetweenGroups(group1.id, group2.id);

    safelyModifyCommunicationToBothOrFail(group2);
    safelyModifyCommunicationToBothOrFail(group1);

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, {[`${testName} Visible should contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) !== null } );
    check(visibles, { [`${testName} Visible should contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can\'t communicate A => G1 => G2 => B when communication is NONE for G1 and BOTH G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is NONE for G1 and BOTH G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-none", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-outgoing", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);
    safelyRemoveCommunicationFromBothOrFail(group1);
    addCommunicationBetweenGroups(group1.id, group2.id);

    removeCommunicationOrFail(group1, 'both');
    safelyModifyCommunicationToBothOrFail(group2);

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should not contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  /*******************************************************************************************************
   *  Communication inter Group Ua -> G1 -> G2 -> Ub G2 = INCOMING
   ******************************************************************************************************/

  group('[Communication] Test that we can\'t communicate A => G1 => G2 => B when communication is OUTGOING for G1 and NONE G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is OUTGOING for G1 and NONE G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-outgoing", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-none", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);
    safelyRemoveCommunicationFromBothOrFail(group1);
    addCommunicationBetweenGroups(group1.id, group2.id);

    modifyCommunicationRelationOrFail(group1, 'outgoing');
    removeCommunicationOrFail(group2, 'both');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });


  group('[Communication] Test that we can communicate A => G1 => G2 => B when communication is INCOMING for G1 and NONE G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is INCOMING for G1 and NONE G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-incoming", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-none", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);
    safelyRemoveCommunicationFromBothOrFail(group1);
    addCommunicationBetweenGroups(group1.id, group2.id);

    removeCommunicationOrFail(group2, 'both');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can communicate A => G1 => G2 when communication is BOTH for G1 and NONE G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is BOTH for G1 and NONE G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-both", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-none", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);
    safelyRemoveCommunicationFromBothOrFail(group1);
    addCommunicationBetweenGroups(group1.id, group2.id);

    safelyModifyCommunicationToBothOrFail(group1);
    removeCommunicationOrFail(group2, 'both');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can\'t communicate A => G1 => G2 => B when communication is NONE for G1 and NONE G2', () => {
    const testName = "[  A => G1 => G2 => B when communication is NONE for G1 and NONE G2 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    const group1: Group = createGroupOrFail(data.structure1.name + "inter-groups-g1-none", data.structure1);
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-none", data.structure2);

    addUsersToGroup([school1Teacher.id], group1);
    addUsersToGroup([school2Teacher.id], group2);

    safelyRemoveCommunicationFromBothOrFail(group2);
    safelyRemoveCommunicationFromBothOrFail(group1);
    addCommunicationBetweenGroups(group1.id, group2.id);

    removeCommunicationOrFail(group1, 'both');
    removeCommunicationOrFail(group2, 'both');

    console.log(`Group created ${group1.name}, id ${group1.id}`);
    console.log(`Group created ${group2.name}, id ${group2.id}`);

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the second user`] : visibles => visibles.find( v => v.id === school2Teacher.id ) === undefined } );
    check(visibles, { [`${testName} Visible should not contain the second group`] : visibles => visibles.find( v => v.id === group2.id ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    deleteGroupOrFail(group1);
    deleteGroupOrFail(group2);
  });


  /*******************************************************************************************************
   *  Communication via DEPEND Group Ua -> G1 <-[DEPEND]-G2
   ******************************************************************************************************/

  group('[Communication] Test that we can communicate A => G1 <= DEPEND G2 when communication is INCOMING for G1', () => {
    const testName = "[  A => G1 <= DEPEND G2 when communication is INCOMING for G1 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const profileGroup  = getProfileGroupOfStructure( 'teachers', data.structure1);

    safelyRemoveCommunicationFromBothOrFail({ id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null });
    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'incoming');

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();

    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should contain the group G2 `] : visibles => visibles.find( v => v.displayName === "Parents du groupe CE1." ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'both');
  });

  group('[Communication] Test that we can communicate A => G1 <= DEPEND G2 when communication is BOTH for G1', () => {
    const testName = "[  A => G1 <= DEPEND G2 when communication is BOTH for G1 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();

    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should contain the group G2 `] : visibles => visibles.find( v => v.displayName === "Parents du groupe CE1." ) !== null } );
  });

  group('[Communication] Test that we can communicate A => G1 <= DEPEND G2 when communication is OUTGOING for G1', () => {
    const testName = "[  A => G1 <= DEPEND G2 when communication is OUTGOING for G1 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const profileGroup  = getProfileGroupOfStructure( 'teachers', data.structure1);

    removeCommunicationOrFail({ id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null });
    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'outgoing');

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();

    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the group G2 `] : visibles => visibles.find( v => v.displayName === "Parents du groupe CE1." ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'both');
  });

  group('[Communication] Test that we can communicate A => G1 <= DEPEND G2 when communication is INCOMING for G1', () => {
    const testName = "[  A => G1 <= DEPEND G2 when communication is INCOMING for G1 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const profileGroup  = getProfileGroupOfStructure( 'teachers', data.structure1);

    removeCommunicationOrFail({ id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null });
    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'incoming');

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();

    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should contain the group G2 `] : visibles => visibles.find( v => v.displayName === "Parents du groupe CE1." ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'both');
  });

  group('[Communication] Test that we can communicate A => G1 <= DEPEND G2 when communication is NONE for G1', () => {
    const testName = "[  A => G1 <= DEPEND G2 when communication is NONE for G1 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');

    const profileGroup  = getProfileGroupOfStructure( 'teachers', data.structure1);

    console.log(`Profile group ${profileGroup.id}`);

    removeCommunicationOrFail({ id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null });

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();

    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the group G2 `] : visibles => visibles.find( v => v.displayName === "Parents du groupe CE1." ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'both');
  });


  /*******************************************************************************************************
   *  Communication via DEPEND Group Ua -> G1 -> G2 <-[DEPEND]-G3
   ******************************************************************************************************/

  group('[Communication] Test that we can communicate B => G2 => G1 (profileGroup) <= DEPEND G3 when communication is INCOMING for G1', () => {
    const testName = "[ B => G2 => G1 (profileGroup) <= DEPEND G3 when communication is INCOMING for G1 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const profileGroup  = getProfileGroupOfStructure( 'teachers', data.structure1);

    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-incoming", data.structure2);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'incoming');

    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group2.id, profileGroup.id);

    <Session>authenticateWeb(school2Teacher.login);

    let visiblesRes = searchVisiblesOrFail();

    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should contain the group G3 `] : visibles => visibles.find( v => v.displayName === "Parents du groupe CE1." ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'both');
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can communicate B => G2 => G1 (profileGroup) <= DEPEND G3 when communication is BOTH for G1', () => {
    const testName = "[  B => G2 => G1 (profileGroup) <= DEPEND G3 when communication is BOTH for G1 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const profileGroup  = getProfileGroupOfStructure( 'teachers', data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-both", data.structure2);
    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'both');

    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group2.id, profileGroup.id);

    <Session>authenticateWeb(school2Teacher.login);

    let visiblesRes = searchVisiblesOrFail();

    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should contain the group G3 `] : visibles => visibles.find( v => v.displayName === "Parents du groupe CE1." ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'both');
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can communicate B => G2 => G1 (profileGroup) <= DEPEND G3 when communication is OUTGOING for G1', () => {
    const testName = "[  B => G2 => G1 (profileGroup) <= DEPEND G3 when communication is OUTGOING for G1 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const profileGroup  = getProfileGroupOfStructure( 'teachers', data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-outgoing", data.structure2);
    safelyRemoveCommunicationFromBothOrFail(group2);

    addCommunicationBetweenGroups(group2.id, profileGroup.id);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null,
      internalCommunicationRule:null, structures: null }, 'outgoing');
    console.log(`Profile group ${profileGroup.id}`);
    <Session>authenticateWeb(school2Teacher.login);

    let visiblesRes = searchVisiblesOrFail();

    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should contain the group G3 `] : visibles => visibles.find( v => v.displayName === "Parents du groupe CE1." ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'both');
    deleteGroupOrFail(group2);
  });

  group('[Communication] Test that we can communicate B => G2 => G1 (profileGroup) <= DEPEND G3 when communication is NONE for G1', () => {
    const testName = "[  B => G2 => G1 (profileGroup) <= DEPEND G3 when communication is NONE for G1 ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const profileGroup  = getProfileGroupOfStructure( 'teachers', data.structure1);
    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');
    const group2: Group = createGroupOrFail(data.structure2.name + "inter-groups-g2-none", data.structure2);
    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'none');

    safelyRemoveCommunicationFromBothOrFail(group2);
    addCommunicationBetweenGroups(group2.id, profileGroup.id);

    <Session>authenticateWeb(school2Teacher.login);

    let visiblesRes = searchVisiblesOrFail();

    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should contain the group G3 `] : visibles => visibles.find( v => v.displayName === "Parents du groupe CE1." ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    modifyCommunicationRelationOrFail( { id : profileGroup.id, name:null,filter: null, internalCommunicationRule:null, structures: null }, 'both');
    deleteGroupOrFail(group2);
  });

  /*******************************************************************************************************
   *  Communication via COMMUNIQUE DIRECT U1 -> U2
   ******************************************************************************************************/

  group('[Communication] Test that we can communicate by direct link A -> B  when A -> B exists', () => {
    const testName = "[ Test that we can communicate by direct link A -> B  when A -> B exists ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');
    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    setDirectCommunicationOrFail(school1Teacher.id, school2Teacher.id, 'both');

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should contain the user B `] : visibles => visibles.find( v => v.id === school2Teacher.id  ) !== null } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    setDirectCommunicationOrFail(school1Teacher.id, school2Teacher.id, 'none');
  });


  group('[Communication] Test that we can\'t communicate by direct link A -> B  when A -> B doesn\'t exists', () => {
    const testName = "[ Test that we can\'t communicate by direct link A -> B  when A -> B doesn\'t exists ]";
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    const school1Users = getUsersOfSchool(data.structure1);
    const school1Teacher = getRandomUserWithProfile(school1Users, 'Teacher');
    const school2Users = getUsersOfSchool(data.structure2);
    const school2Teacher = getRandomUserWithProfile(school2Users, 'Teacher');

    setDirectCommunicationOrFail(school1Teacher.id, school2Teacher.id, 'incoming');

    <Session>authenticateWeb(school1Teacher.login);

    let visiblesRes = searchVisiblesOrFail();
    const visibles: Visible[] = JSON.parse(<string>visiblesRes.body);
    check(visibles, { [`${testName} Visible should not contain the user B `] : visibles => visibles.find( v => v.id === school2Teacher.id  ) === undefined } );

    //tear down
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    setDirectCommunicationOrFail(school1Teacher.id, school2Teacher.id, 'none');
  });

}
