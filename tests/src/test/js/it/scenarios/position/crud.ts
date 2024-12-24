import { check } from "k6";
import {chai, describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import {
  authenticateWeb,
  assertCondition,
  assertOk,
  searchPositions,
  createPosition,
  createPositionOrFail,
  updatePosition,
  getUsersOfSchool,
  initStructure,
  logout,
  deletePosition,
  getRandomUserWithProfile,
  attachStructureAsChild,
  getAdmlsOrMakThem,
  getSearchCriteria,
  checkReturnCode,
  getOrCreatePosition,
  attachUserToStructures,
  getRandomUser,
  switchSession,
  getPositionByIdOrFail,
  Session,
  UserPosition,
  mergeUsers,
  getPositionsOfStructure
} from "../../../node_modules/edifice-k6-commons/dist/index.js";


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
    renamePosition: {
      exec: 'testRenamePosition',
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
    testGetPositionsAfterMergingAdml: {
      exec: 'testGetPositionsAfterMergingAdml',
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
    let session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    adml = getAdmlsOrMakThem(structure, 'Teacher', 1, [])[0]


    const schoolName = `IT Positions - NEtab`
    //////////////////////////////////
    // Create 1 head structure and 2
    // depending structures
    const chapeau = initStructure(`Chapeau - ${schoolName}${suffix}`, 'tiny')
    const structure1 = initStructure(`1 - ${schoolName}${suffix}`, 'tiny')
    const structure2 = initStructure(`2 - ${schoolName}${suffix}`, 'tiny')
    attachStructureAsChild(chapeau, structure1)
    attachStructureAsChild(chapeau, structure2)
    ////////////////////////////////////
    // Create 1 ADML for each structure
    // and 1 ADML for the head structure
    const megaAdml = getAdmlsOrMakThem(chapeau, 'Teacher', 1, [])[0]
    const adml1 = getAdmlsOrMakThem(structure1, 'Teacher', 1, [megaAdml])[0]
    const adml2 = getAdmlsOrMakThem(structure2, 'Teacher', 1, [megaAdml])[0]
    attachUserToStructures(megaAdml, [structure1, structure2])

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
    let session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const positionName = "IT Position - Create - " + Date.now();
    const users = getUsersOfSchool(structure)
    const teacher = getRandomUserWithProfile(users, 'Teacher', [adml]);

    logout()
    let res = createPosition(positionName, structure);
    assertCondition(() => res.status === 401, "An unauthenticated user should not be able to create a position");

    session = <Session>authenticateWeb(teacher.login)
    res = createPosition(positionName, structure);
    assertCondition(() => res.status === 401, "An authenticated user without special rights should not be able to create a position");


    session = <Session>authenticateWeb(adml.login)
    let positions: UserPosition[] = []
    res = createPosition(positionName, structure);
    assertCondition(() => res.status === 201, "An ADML user should be able to create a position");
    positions.push(<UserPosition>JSON.parse(<string>res.body));
    res = createPosition(positionName, structure);
    checkReturnCode(res, "A position cannot be created multiple times", 409);
    res = createPosition(`${positionName} - bis`, structure);
    positions.push(JSON.parse(<string>res.body));
    //assertSearchCriteriaContainSpecifiedPositionsAndNotOther(positions, p => p.structureId !==  structure.id, "ADML with a structure with these positions");
    session = <Session>authenticateWeb(adml1.login)

    res = createPosition(`${positionName}-ADML2`, structure);
    assertCondition(() => res.status === 401, "An ADML of another structure should not be able to create a position");
    assertSearchCriteriaContainSpecifiedPositionsAndNotOther([], p => p.structureId !==  structure.id, "ADML in a structure without these positions");

    // An ADMC should be able to create a position
    session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    createPositionOrFail(`${positionName}-ADMC`, structure1);

    ////////////////////////////////////////////////////////////////////////////////
    // An ADML of multiple structures should be able to see the positions of all
    // administered structures
    // ADML of each structure create position(s) on their respective structure and
    // then the head adml fetches the searchCriteria and we check if the previously
    // created positions are accessible
    positions = []
    session = <Session>authenticateWeb(adml1.login);
    positions.push(createPositionOrFail(`${positionName}-ADML1-0`, structure1));
    positions.push(createPositionOrFail(`${positionName}-ADML1-1`, structure1));
    session = <Session>authenticateWeb(adml2.login);
    positions.push(createPositionOrFail(`${positionName}-ADML2-0`, structure2));
    session = <Session>authenticateWeb(headAdml.login);
    assertSearchCriteriaContainSpecifiedPositionsAndNotOther(positions, p => p.structureId ===  structure1.id || p.structureId ===  structure2.id, "ADML of multiple structures");
})
};


export function testRenamePosition({structure, adml, structureTree}) {
  const {admls: [adml1, adml2], structures: [structure1, structure2], headAdml} = structureTree
  describe("[Position-CRUD] Rename position", () => {
    let session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users1 = getUsersOfSchool(structure1)
    const unprivilegedUserOfStructure1 = getRandomUser(users1, [adml1, headAdml])
    const users2 = getUsersOfSchool(structure2)
    const unprivilegedUserOfStructure2 = getRandomUser(users2, [adml2, headAdml])
    const adml1Session = <Session>authenticateWeb(adml1.login)
    const positionName = "IT Position - Rename - To rename" + Date.now();
    const positionToRename = createPositionOrFail(positionName, structure1)
    let previousName = positionName
    const usersThatCanRename: [Session, string][] = [
      [<Session>authenticateWeb(adml1.login), "ADML of administered structure"],
      [<Session>authenticateWeb(headAdml.login), "ADML of head structure"],
      [<Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD), "ADMC"]
    ]
    for(let userThatCanRename of usersThatCanRename) {
      let [sessionToTry, label] = userThatCanRename
      describe(`${label} - Can rename positions`, () => {
        switchSession(sessionToTry)
        const successfullRename = `${positionName} - renamed by ${label}`
        positionToRename.name = successfullRename
        assertOk(updatePosition(positionToRename), `should be able to rename a position`)
        switchSession(adml1Session)
        let res = searchPositions(positionName)
        check(JSON.parse(<string>res.body), {
          'should only find the renamed position': positions => positions.length === 1,
          'position should be renamed': positions => positions[0].name === successfullRename
        })
        previousName = successfullRename
      })
    }
    const usersThatCannotRename = [
      [<Session>authenticateWeb(adml2.login), "ADML of an unadministered structure", 403],
      [<Session>authenticateWeb(unprivilegedUserOfStructure1.login), "Unprivileged user of the structure of the position", 401],
      [<Session>authenticateWeb(unprivilegedUserOfStructure2.login), "Unprivileged user of an unrelated structure", 401],
      [null, "Unauthenticated user", 302]
    ]
    for(let user of usersThatCannotRename) {
      let [sessionToTry, label, expectedHTTPErrorCode] = user
      describe(`${label} - Cannot rename positions`, () => {
        if(sessionToTry === null) {
          logout()
        }
        const failedRename = `${positionName} - renamed by ${label}`
        positionToRename.name = failedRename
        switchSession(<Session>sessionToTry)
        checkReturnCode(
          updatePosition(positionToRename),
          `should not be able to rename a position`,
          <number>expectedHTTPErrorCode)
        switchSession(adml1Session)
        let res = searchPositions(positionName,)
        check(JSON.parse(<string>res.body), {
          'should only find the original position': positions => positions.length === 1,
          'position should not be renamed': positions => positions[0].name === previousName
        })
      })
    }
    describe('Forbidden actions', () => {
      session = <Session>authenticateWeb(headAdml.login)
      const fixedName = `${positionName} - fixed`
      const fixedNameInOtherStructure = `${positionName} - fixed in structure 2`
      createPositionOrFail(fixedName, structure1)
      createPositionOrFail(fixedNameInOtherStructure, structure2)
      positionToRename.name = fixedName
      checkReturnCode(
        updatePosition(positionToRename),
        `should not allow to rename a position with a name that already exists in the structure`,
        409
      )
      check(getPositionByIdOrFail(positionToRename.id), {
        'should not be able to reuse a name of a position in the same structure' : p => p.name === previousName,
      })
      positionToRename.name = fixedNameInOtherStructure
      assertOk(updatePosition(positionToRename), 'should allow to reuse a name of a position in another structure')
      check(getPositionByIdOrFail(positionToRename.id), {
        'should be able to reuse a name of a position in another structure' : p => p.name === fixedNameInOtherStructure,
        'source should not have been modified' : p => p.source === 'MANUAL'
      })
    })
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
    let session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    createPosition(`IT Position - Search - Coucou`, structure)
    createPosition(`IT Position - Search - Hello`, structure)
    createPosition(`IT Position - Search - HellÔ les amis`, structure)
    createPosition(`IT Position - Search - Hello les amis coucou`, structure)
  
    
    const users = getUsersOfSchool(structure)
    const teacher = getRandomUserWithProfile(users, 'Teacher', [adml]);

    let res = searchPositions('Coucou');
    assertCondition(() => res.status === 401, "An unauthenticated user should not be able to search positions");


    session = <Session>authenticateWeb(teacher.login)
    res = searchPositions('Coucou');
    assertCondition(() => res.status === 401, "An authenticated user without special rights should not be able to search positions");


    session = <Session>authenticateWeb(adml.login)
    res = searchPositions('Coucou');
    assertOk(res, "An ADML should be able to search positions");
    res = searchPositions('hello');
    check(JSON.parse(<string>res.body), {
      'search should return all positions containing the filter': pos => pos && pos.length === 3,
      'search should be case-insensitive': pos => pos.filter(p => p.name.includes('Hello')).length == 2,
      'search should be special-character-insensitive': pos => pos.filter(p => p.name.includes('HellÔ')).length == 1,
      'no duplicates were returned': pos =>   noDuplicates(pos)
    })


    session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    res = searchPositions('Coucou');
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
    let session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    //////////////////////////////////
    // Search positions
    //////////////////////////////////
    // Create positions for structures
    // 1 and separately
    const positions1 = [
      getOrCreatePosition(`IT Position - Search - MEtab1 Coucou`, structure1),
      getOrCreatePosition(`IT Position - Search - MEtab1 Hello`, structure1),
      getOrCreatePosition(`IT Position - Search - MEtab1 Hello les amis`, structure1),
      getOrCreatePosition(`IT Position - Search - MEtab1 Hello les amis coucou`, structure1)
    ]
    const positions2 = [
      getOrCreatePosition(`IT Position - Search - MEtab2 Coucou`, structure2),
      getOrCreatePosition(`IT Position - Search - MEtab2 Hello`, structure2),
      getOrCreatePosition(`IT Position - Search - MEtab2 Hello les amis`, structure2)
    ]
    session = <Session>authenticateWeb(adml1.login)

    let res = searchPositions('IT Position - Search - MEtab');
    check(JSON.parse(<string>res.body), {
      'all positions of structure1 should be fetched': pos => allPositionsOk(positions1, pos),
      'no duplicates were returned': pos =>   noDuplicates(pos)
    })
    res = searchPositions('IT Position - Search - MEtab2');
    check(JSON.parse(<string>res.body), {
      'adml of structure1 should see no positions of structure 2': pos => pos && pos.length === 0
    })

    session = <Session>authenticateWeb(adml2.login)
    res = searchPositions('IT Position - Search - MEtab');
    check(JSON.parse(<string>res.body), {
      'all positions of structure2 should be fetched': pos => allPositionsOk(positions2, pos),
      'no duplicates were returned': pos => noDuplicates(pos)
    })

    session = <Session>authenticateWeb(headAdml.login)
    res = searchPositions('IT Position - Search - MEtab');
    check(JSON.parse(<string>res.body), {
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
    let session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    /////////////////////////////////////
    // Create 2 positions for structure 1
    // and 3 positions for structure 2
    /////////////////////////////////////
    const postionEtab1_1 = getOrCreatePosition(`IT Position - Delete - MEtab1 01`, structure1);
    const postionEtab1_2 = getOrCreatePosition(`IT Position - Delete - MEtab1 02`, structure1);
    const postionEtab2_1 = getOrCreatePosition(`IT Position - Delete - MEtab2 01`, structure2);
    const postionEtab2_2 = getOrCreatePosition(`IT Position - Delete - MEtab2 02`, structure2);
    const postionEtab2_3 = getOrCreatePosition(`IT Position - Delete - MEtab2 03`, structure2);
    
    //////////////////////////////////
    // Get "random" users
    //////////////////////////////////
    const users = getUsersOfSchool(structure1)
    const teacher = getRandomUserWithProfile(users, 'Teacher', [adml1]);


    /////////////////////////////////
    // Tests
    /////////////////////////////////
    logout();
    let res = deletePosition(postionEtab2_1.id);
    assertCondition(() => res.status === 401, "An unauthenticated user should not be able to delete a position");
    session = <Session>authenticateWeb(teacher.login)
    res = deletePosition(postionEtab2_1.id);
    assertCondition(() => res.status === 401, "A user who is not ADML should not be able to delete a position");
    session = <Session>authenticateWeb(adml1.login)
    res = deletePosition(postionEtab2_1.id);
    assertCondition(() => res.status === 401, "An ADML of another structure should not be able to delete this position");
    res = deletePosition(postionEtab1_1.id);
    assertOk(res, "An ADML of a structure should be able to delete a position");

    session = <Session>authenticateWeb(headAdml.login)
    res = deletePosition(postionEtab1_2.id);
    assertOk(res, "An ADML of a head structure should be able to delete a position of an administered structure");

    session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    res = deletePosition(postionEtab2_2.id);
    assertOk(res, "An ADMC should be able to delete any position");

    session = <Session>authenticateWeb(headAdml.login);
})
};

// Integration Test to specifically check that when two adml are merged
// then we still can correctly retrieve the positions related to the
// structure of the resulting adml.
// It covers the bugfix of WB-3640
export function testGetPositionsAfterMergingAdml({structureTree}) {
  const {structures: [structure1], admls: [adml1], headAdml} = structureTree
  describe("", () => {
    authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);

    ////////////////////////////////////
    // Create 2 ADML for structure 1
    // and merge them
    const admlToMerge1 = getAdmlsOrMakThem(structure1, 'Teacher', 1, [headAdml, adml1])[0]
    const admlToMerge2 = getAdmlsOrMakThem(structure1, 'Teacher', 1, [headAdml, adml1, admlToMerge1])[0]

    console.log("ID structure 1 : ", structure1.id)
    console.log("ADML ids : ", admlToMerge1.id, " and ", admlToMerge2.id)
    mergeUsers(admlToMerge1.id, admlToMerge2.id, true)

    authenticateWeb(admlToMerge1.login)
    let res = getPositionsOfStructure(structure1)

    assertOk(res, "An ADML that has got merged with another ADML account should be able to retrieve the user positions of its structure.")
  })
}

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

function assertSearchCriteriaContainSpecifiedPositionsAndNotOther(expected, unwantedPredicate, userType) {
  describe(userType, () => {
    const criteria = getSearchCriteria();
    const criteriaPositions = criteria.positions || [];
    const actualIds = criteriaPositions.map(e => e.id)
    const ok = check(criteriaPositions, {
      'should contain all expected positions' : () => expected.filter(exp => actualIds.indexOf(exp.id) < 0).length === 0,
      'should contain none of the unwanted positions' : () => actualIds.filter(act => unwantedPredicate({id: act})).length >= 0
    });
    if(!ok) {
      console.warn("actualIds", actualIds)
      console.warn('Expecting positions\n', expected, '\ngot\n', criteriaPositions)
    }
  })
}