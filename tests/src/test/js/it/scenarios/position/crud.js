import { check } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import {
  authenticateWeb,
  assertCondition,
  assertOk,
  makeAdml,
  searchPositions,
  createPosition,
  createPositionOrFail,
  getUsersOfSchool,
  createEmptyStructure,
  initStructure,
  logout,
  deletePosition,
  getRandomUserWithProfile,
  attachStructureAsChild,
  getAdmlsOrMakThem,
  getSearchCriteria
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";


chai.config.logFailures = true;


export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    createPosition: {
      exec: 'testCreatePosition',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '1s',
    },
    deletePosition: {
      exec: 'testDeletePosition',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '1s'
    },
    searchPositionsOnOneEtab: {
      exec: 'searchPositionsOnOneEtab',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '1s'
    },
    searchPositionsOnMultipleEtabs: {
      exec: 'searchPositionsOnMultipleEtabs',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '1s'
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
    const suffix = __ENV.RECREATE_STRUCTURES === 'true' ? ` - ${Date.now()}` : ''
    structure = initStructure(`IT - Fonctions${suffix}`, 'tiny');
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    adml = getAdmlsOrMakThem(structure, 'Teacher', 1, session)[0]


    const schoolName = `IT Positions - NEtab`
    //////////////////////////////////
    // Create 1 head structure and 2
    // depending structures
    const chapeau = createEmptyStructure(`Chapeau - ${schoolName}${suffix}`, false, session)
    const structure1 = initStructure(`1 - ${schoolName}${suffix}`, 'tiny')
    const structure2 = initStructure(`2 - ${schoolName}${suffix}`, 'tiny')
    const structure3 = initStructure(`3 - ${schoolName}${suffix}`, 'tiny')
    attachStructureAsChild(chapeau, structure1, session)
    attachStructureAsChild(chapeau, structure2, session)
    ////////////////////////////////////
    // Create 1 ADML for each structure
    // and 1 ADML for the head structure
    const megaAdml = getAdmlsOrMakThem(structure3, 'Teacher', 1, session)[0]
    makeAdml(megaAdml, chapeau, session)
    const adml1 = getAdmlsOrMakThem(structure1, 'Teacher', 1, [megaAdml], session)[0]
    const adml2 = getAdmlsOrMakThem(structure2, 'Teacher', 1, [megaAdml], session)[0]
    structureTree = { head: chapeau, structures: [structure1, structure2], admls: [adml1, adml2], headAdml: megaAdml}
  });
  return { structure, adml, structureTree };
}
/**
 * Ensure that :
 * - unauthenticated users cannot create positions
 * - non-adml users cannot create positions
 * - adml can create positions only in the structures they administer
 * - admc can create positions on avery structures
 * - search criteria return positions accessible by the user
 * @param {*} param0 Initialized data
 */
export function testCreatePosition({structure, adml, structureTree}) {
  const {admls: [adml1, adml2], structures: [structure1, structure2], headAdml} = structureTree
  describe("[Position-CRUD] Create positions", () => {
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const positionName = "IT Position - Create - " + Date.now();
    const users = getUsersOfSchool(structure, session)
    const teacher = getRandomUserWithProfile(users, 'Teacher', [adml]);

    logout(session)
    let res = createPosition(positionName, structure);
    assertCondition(() => res.status === 401, "An unauthenticated user should not be able to create a position");

    session = authenticateWeb(teacher.login)
    res = createPosition(positionName, structure, session);
    assertCondition(() => res.status === 401, "An authenticated user without special rights should not be able to create a position");


    session = authenticateWeb(adml.login)
    let positions = []
    res = createPosition(positionName, structure, session);
    assertCondition(() => res.status === 201, "An ADML user should be able to create a position");
    positions.push(JSON.parse(res.body));
    res = createPosition(`${positionName} - bis`, structure, session);
    assertCondition(() => res.status === 201, "An ADML user should be able to create a position");
    positions.push(JSON.parse(res.body));
    //assertSearchCriteriaContainSpecifiedPositionsAndNotOther(positions, p => p.structureId !==  structure.id, "ADML with a structure with these positions", session);
    session = authenticateWeb(adml1.login)

    res = createPosition(`${positionName}-ADML2`, structure, session);
    assertCondition(() => res.status === 401, "An ADML of another structure should not be able to create a position");
    assertSearchCriteriaContainSpecifiedPositionsAndNotOther([], p => p.structureId !==  structure.id, "ADML in a structure without these positions", session);

    // An ADMC should be able to create a position
    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    createPositionOrFail(`${positionName}-ADMC`, structure1, session);

    ////////////////////////////////////////////////////////////////////////////////
    // An ADML of multiple structures should be able to see the positions of all
    // administered structures
    // ADML of each structure create position(s) on their respective structure and
    // then the head adml fetches the searchCriteria and we check if the previously
    // created positions are accessible
    positions = []
    session = authenticateWeb(adml1.login);
    positions.push(createPositionOrFail(`${positionName}-ADML1-0`, structure1, session));
    positions.push(createPositionOrFail(`${positionName}-ADML1-1`, structure1, session));
    session = authenticateWeb(adml2.login);
    positions.push(createPositionOrFail(`${positionName}-ADML2-0`, structure2, session));
    session = authenticateWeb(headAdml.login);
    assertSearchCriteriaContainSpecifiedPositionsAndNotOther(positions, p => p.structureId ===  structure1.id || p.structureId ===  structure2.id, "ADML of multiple structures", session);
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
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    createPositionOrFail(`IT Position - Search - Coucou`, structure, session)
    createPositionOrFail(`IT Position - Search - Hello`, structure, session)
    createPositionOrFail(`IT Position - Search - HellÔ les amis`, structure, session)
    createPositionOrFail(`IT Position - Search - Hello les amis coucou`, structure, session)
  
    
    const users = getUsersOfSchool(structure, session)
    const teacher = getRandomUserWithProfile(users, 'Teacher', [adml]);

    let res = searchPositions('Coucou');
    assertCondition(() => res.status === 401, "An unauthenticated user should not be able to search positions");


    session = authenticateWeb(teacher.login)
    res = searchPositions('Coucou', session);
    assertCondition(() => res.status === 401, "An authenticated user without special rights should not be able to search positions");


    session = authenticateWeb(adml.login)
    res = searchPositions('Coucou', session);
    assertOk(res, "An ADML should be able to search positions");
    res = searchPositions('hello', session);
    check(JSON.parse(res.body), {
      'search should return all positions containing the filter': pos => pos && pos.length === 3,
      'search shoud be case-insensitive': pos => pos.filter(p => p.name.contains('Hello')).length == 2,
      'search shoud be special-character-insensitive': pos => pos.filter(p => p.name.contains('HellÔ')).length == 1,
      'no duplicates were returned': pos =>   noDuplicates(pos)
    })


    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    res = searchPositions('Coucou', structure, session);
    assertOk(res, "An ADMC should be able to search positions"); 
})
};
/**
 * Ensure that :
 * - adml of one structure can only see positions of this structure
 * - adml of multiple structures can see positions of all the structure they administer
 * @param {*} param0 Initialized data
 */
export function searchPositionsOnMultipleEtabs({structureTree}) {
  const {structures: [structure1, structure2], admls: [adml1, adml2], headAdml} = structureTree
  describe("[Position-CRUD] Search Positions - Multiple Etab", () => {
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
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

    let res = searchPositions('IT Position - Search - MEtab', session);
    check(JSON.parse(res.body), {
      'adml of structure1 should see the positions of structure 1 only': pos => pos && pos.length === positions1.length,
      'all positions of structure1 should be fetched': pos => allPositionsOk(positions1, pos),
      'no duplicates were returned': pos =>   noDuplicates(pos)
    })
    res = searchPositions('IT Position - Search - MEtab2', session);
    check(JSON.parse(res.body), {
      'adml of structure1 should see no positions of structure 2': pos => pos && pos.length === 0
    })

    session = authenticateWeb(adml2.login)
    res = searchPositions('IT Position - Search - MEtab', session);
    check(JSON.parse(res.body), {
      'adml of structure2 should see the positions of structure 2 only': pos => pos && pos.length === positions2.length,
      'all positions of structure2 should be fetched': pos => allPositionsOk(positions2, pos),
      'no duplicates were returned': pos => noDuplicates(pos)
    })

    session = authenticateWeb(headAdml.login)
    res = searchPositions('IT Position - Search - MEtab', session);
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
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    /////////////////////////////////////
    // Create 2 positions for structure 1
    // and 3 positions for structure 2
    /////////////////////////////////////
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
    assertCondition(() => res.status === 401, "An unauthenticated user should not be able to delete a position");
    session = authenticateWeb(teacher.login)
    res = deletePosition(postionEtab2_1, session);
    assertCondition(() => res.status === 401, "A user who is not ADML should not be able to delete a position");
    session = authenticateWeb(adml1.login)
    res = deletePosition(postionEtab2_1, session);
    assertCondition(() => res.status === 401, "An ADML of another structure should not be able to delete this position");
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
  for(const p of actual) {
    actualIds.add(p.id)
  }
  const notFound = expected.filter(p => !actualIds.has(p.id))
  if(notFound.length > 0) {
    console.warn(`The following positions were expected but not retrieved : ${notFound.map(e => e.id + '-' + e.name)}`)
  }
  return notFound.length === 0;
}

function noDuplicates(positions) {
  const actualIds = new Set();
  for(const p of positions) {
    actualIds.add(p.id)
  }
  return positions.length === actualIds.size;
}

function assertSearchCriteriaContainSpecifiedPositionsAndNotOther(structureId, expected, unwantedPredicate, userType) {
  describe(userType, () => {
    const criteria = getSearchCriteria();
    const criteriaPositions = criteria.positions || [];
    const actualIds = criteriaPositions.map(e => e.id)
    const ok = check(criteriaPositions, {
      'should contain all expected positions' : () => expected.filter(exp => actualIds.indexOf(exp.id) < 0).length === 0,
      'should contain none of the unwanted positions' : () => actualIds.filter(act => unwantedPredicate({id: act.id})).length >= 0
    });
    if(!ok) {
      console.warn("actualIds", actualIds)
      console.warn('Expecting positions\n', expected, '\ngot\n', criteriaPositions)
    }
  })
}