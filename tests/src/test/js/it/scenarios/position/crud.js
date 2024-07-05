import { check } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import {
  authenticateWeb,
  assertKo,
  assertOk,
  makeADML,
  searchPositions,
  createDefaultStructure,
  createPosition,
  createPositionOrFail,
  getUsersOfSchool,
  createEmptyStructure,
  initStructure,
  logout,
  deletePosition,
  getRandomUserWithProfile
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";


chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    createPosition: {
      exec: 'testCreatePosition',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "60s",
      gracefulStop: '60s',
    },
    searchPositionsOnOneEtab: {
      exec: 'searchPositionsOnOneEtab',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "60s",
      gracefulStop: '60s',
    },
    searchPositionsOnMultipleEtabs: {
      exec: 'searchPositionsOnMultipleEtabs',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "60s",
      gracefulStop: '60s',
    },
    deletePosition: {
      exec: 'testDeletePosition',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "60s",
      gracefulStop: '60s',
    },
  },
};

/**
 * @returns A test dataset containing
 * - structure: a standalone structure
 * - adml: an ADML on this structure
 * - structureTree: 
 *    - head: a head structure containing the following 2
 *    - headAdml: ADML of the head structure
 *    - structures: a list structures depending on the head structure
 *    - admls: a list of adml (one per structure and they are in the same order of the structures)
 */
export function setup() {
  let structure;
  let adml;
  let structureTree;
  describe("[Position-CRUD] Initialize data", () => {
    //////////////////////////////////////////////////////
    // Create a simple structure and create one ADML for it
    structure = createDefaultStructure();
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(structure, session)
    adml = getRandomUserWithProfile(users, 'Teacher');
    makeADML(adml, structure, session)


    const schoolName = `IT Positions - NEtab ${Date.now()}`

    //////////////////////////////////
    // Create 1 head structure and 2
    // depending structures
    const chapeau = createEmptyStructure(`Chapeau - ${schoolName}`, false, session)
    const structure1 = initStructure(`1 - ${schoolName}`, session)
    const structure2 = initStructure(`2 - ${schoolName}`, session)
    attachStructureAsChild(chapeau, structure1, session)
    attachStructureAsChild(chapeau, structure2, session)
    ////////////////////////////////////
    // Create 1 ADML for each structure
    // and 1 ADML for the head structure
    const users1 = getUsersOfSchool(structure1, session)
    const adml1 = getRandomUserWithProfile(users1, 'Teacher');
    makeADML(adml1, structure1, session)
    const users2 = getUsersOfSchool(structure2, session)
    const adml2 = getRandomUserWithProfile(users2, 'Teacher');
    makeADML(adml2, structure2, session)
    const megaAdml = getRandomUserWithProfile(users2, 'Teacher', [adml2]);
    makeADML(megaAdml, structure, session)
    structureTree = { head: chapeau, structures: [structure1, structure2], admls: [structure1, structure2], headAdml: megaAdml}
  });
  return { structure, adml, structureTree };
}
/**
 * Ensure that :
 * - unauthenticated users cannot create positions
 * - non-adml users cannot create positions
 * - adml can create positions only in the structures they administer
 * - admc can create positions on avery structures
 * @param {*} param0 Initialized data
 */
export function testCreatePosition({structure, adml, structureTree}) {
  const {admls: [adml1]} = structureTree
  describe("[Position-CRUD] Create positions", () => {
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const positionName = "IT Position - Create - " + Date.now();
    const users = getUsersOfSchool(structure, session)
    const teacher = getRandomUserWithProfile(users, 'Teacher', [adml]);

    logout(session)
    let res = createPosition(positionName, structure);
    assertKo(res, "An unauthenticated user should not be able to create a position");

    session = authenticateWeb(teacher.login)
    res = createPosition(positionName, structure, session);
    assertKo(res, "An authenticated user without special rights should not be able to create a position");


    session = authenticateWeb(adml.login)
    res = createPosition(positionName, structure, session);
    assertKo(res, "An ADML user should not be able to create a position");


    session = authenticateWeb(adml1.login)
    res = createPosition(positionName, structure, session);
    assertKo(res, "An ADML of another structure should not be able to create a position");

    // An ADMC should be able to create a position
    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    createPositionOrFail(positionName, structure, session);
})
};
/**
 * Ensure that :
 * - unauthenticated users cannot see positions
 * - non-adml users cannot see positions
 * - admx can see positions
 * @param {*} param0 Initialized data
 */
export function searchPositionsOnOneEtab({structure, adml}) {
  describe("[Position-CRUD] Search Positions - One Etab", () => {
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    createPositionOrFail(`IT Position - Search - Coucou`, structure, session)
    createPositionOrFail(`IT Position - Search - Hello`, structure, session)
    createPositionOrFail(`IT Position - Search - Hello les amis`, structure, session)
    createPositionOrFail(`IT Position - Search - Hello les amis coucou`, structure, session)
  
    
    const users = getUsersOfSchool(structure, session)
    const teacher = getRandomUserWithProfile(users, 'Teacher', [adml]);

    let res = searchPositions('Coucou');
    assertKo(res, "An unauthenticated user should not be able to search positions");


    session = authenticateWeb(teacher.login)
    res = searchPositions('Coucou', session);
    assertKo(res, "An authenticated user without special rights should not be able to search positions");


    session = authenticateWeb(asml.login)
    res = searchPositions('Coucou', session);
    assertOk(res, "An ADML should be able to search positions");


    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    res = searchPositions('Coucou', structure, session);
})
};
/**
 * Ensure that :
 * - adml of one structure can only see positions of this structure
 * - adml of multiple structures can see positions of all the structure they administer
 * @param {*} param0 Initialized data
 */
export function searchPositionsOnMultipleEtabs({structureTree}) {
  const {structures: [structure1, structure2], admls: [adml1, adml2]} = structureTree
  describe("[Position-CRUD] Search Positions - Multiple Etab", () => {
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    //////////////////////////////////
    // Search positions
    //////////////////////////////////
    // Create positions for structures
    // 1 and separately
    const positions1 = [
      createPositionOrFail(`IT Position - Search - MEtab1 Coucou`, structure1, session),
      createPositionOrFail(`IT Position - Search - MEtab1 Hello`, structure1, session),
      createPositionOrFail(`IT Position - Search - MEtab1 Hello les amis`, structure1, session),
      createPositionOrFail(`IT Position - Search - MEtab1 Hello les amis coucou`, structure1, session)
    ]
    const positions2 = [
      createPositionOrFail(`IT Position - Search - MEtab2 Coucou`, structure2, session),
      createPositionOrFail(`IT Position - Search - MEtab2 Hello`, structure2, session),
      createPositionOrFail(`IT Position - Search - MEtab2 Hello les amis`, structure2, session)
    ]

    session = authenticateWeb(adml1.login)

    let res = searchPositions('MEtab1', session);
    check(JSON.parse(res.body), {
      'adml of structure1 should see the positions of structure 1 only': pos => pos && pos.length === positions1.length,
      'all positions of structure1 should be fetched': pos => allPositionsOk(positions1, pos),
      'no duplicates were returned': pos =>   (positions1)
    })

    session = authenticateWeb(adml2.login)
    res = searchPositions('MEtab2', session);
    check(JSON.parse(res.body), {
      'adml of structure2 should see the positions of structure 2 only': pos => pos && pos.length === positions2.length,
      'all positions of structure1 should be fetched': pos => allPositionsOk(positions2, pos),
      'no duplicates were returned': pos => noDuplicates(positions2)
    })

    session = authenticateWeb(megaAdml.login)
    res = searchPositions('MEtab', session);
    check(JSON.parse(res.body), {
      'adml of structure1 and structure2 should see the positions of both structures': pos => pos && pos.length === (positions2.length + positions1.length),
      'all positions of structure1 should be fetched': pos => allPositionsOk([...positions2, ...positions1], pos),
      'no duplicates were returned': pos => noDuplicates([...positions2, ...positions1])
    })
})
};

/**
 * Tests that :
 * - unauthenticated users cannot delete positions
 * - adml users can delete positions on structures they can administer
 * - admc can delete all positions
 * @param {*} param0 initial data 
 */
export function testDeletePosition({structureTree}) {
  const {structures: [structure1, structure2], admls: [adml1], headAdml} = structureTree
  describe("[Position-CRUD] Delete position", () => {
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    ////////////////////////////////////
    // Create 2 positions for structure
    // 1 and 1 position for structure 2
    ////////////////////////////////////
    const postionEtab1_1 = createPositionOrFail(`IT Position - Delete - MEtab1 01`, structure1, session);
    const postionEtab1_2 = createPositionOrFail(`IT Position - Delete - MEtab1 02`, structure1, session);
    const postionEtab2_1 = createPositionOrFail(`IT Position - Delete - MEtab2 01`, structure2, session);
    const postionEtab2_2 = createPositionOrFail(`IT Position - Delete - MEtab2 02`, structure2, session);
    const postionEtab2_3 = createPositionOrFail(`IT Position - Delete - MEtab2 03`, structure2, session);
    
    //////////////////////////////////
    // Get "random" users
    //////////////////////////////////
    const users = getUsersOfSchool(structure1, session)
    const teacher = getRandomUserWithProfile(users, 'Teacher', [adml1]);


    /////////////////////////////////
    // Tests
    /////////////////////////////////
    logout(session);
    let res = deletePosition(postionEtab2_1);
    assertKo(res, "An unauthenticated user should not be able to delete a position");
    session = authenticateWeb(teacher.login)
    res = deletePosition(postionEtab2_1, session);
    assertKo(res, "A user who is not ADML should not be able to delete a position");
    session = authenticateWeb(adml.login)
    res = deletePosition(postionEtab2_1, session);
    assertKo(res, "An ADML of another structure should not be able to delete this position");
    res = deletePosition(postionEtab1_1, session);
    assertOk(res, "An ADML of a structure should be able to delete a position");

    session = authenticateWeb(headAdml.login)
    res = deletePosition(postionEtab1_2, session);
    assertOk(res, "An ADML of a head structure should be able to delete a position of an administered structure");

    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    res = deletePosition(postionEtab2_2, session);
    assertOk(res, "An ADMC should be able to delete any position");

    session = authenticateWeb(headAdml.login);
})
};

function allPositionsOk(expected, actual) {
  const actualIds = new Set();
  for(const p in actual) {
    actualIds.add(p.id)
  }
  const notFound = expected.filter(p => !actualIds.has(p.id))
  if(notFound.length > 0) {
    console.warn(`The following positions were expected but not retrieved : ${notFound}`)
  }
  return notFound.length === 0;
}

function noDuplicates(positions) {
  const actualIds = new Set();
  for(const p in positions) {
    actualIds.add(p.id)
  }
  return positions.length === actualIds.size();
}