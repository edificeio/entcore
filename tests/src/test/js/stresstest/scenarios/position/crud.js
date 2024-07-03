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
  createEmptyStructure
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";


chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    createPosition: {
      exec: 'createPosition',
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
  },
};


export function setup() {
  let structure;
  let adml;
  describe("[Position-CRUD] Initialize data", () => {
    structure = createDefaultStructure();
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const users = getUsersOfSchool(structure, session)
    adml = getRandomUserWithProfile(users, 'Teacher');
    makeADML(teacher, structure, session)
  });
  return { structure, adml };
}
export function createPosition({structure, adml}) {
  describe("[Position-CRUD] Create a position on an existing structure as an ADMC", () => {
    const positionName = "IT Position - Create - " + Date.now();
    const users = getUsersOfSchool(structure, session)
    const teacher = getRandomUserWithProfile(users, 'Teacher', [adml]);

    let res = createPosition(positionName, structure);
    assertKo(res, "An unauthenticated user should not be able to create a position");


    let session = authenticateWeb(teacher.login)
    res = createPosition(positionName, structure, session);
    assertKo(res, "An authenticated user without special rights should not be able to create a position");


    session = authenticateWeb(adml.login)
    res = createPosition(positionName, structure, session);
    assertKo(res, "An ADML user should not be able to create a position");


    // An ADMC should be able to create a position
    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    createPositionOrFail(positionName, structure, session);
})
};
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
export function searchPositionsOnMultipleEtabs() {
  describe("[Position-CRUD] Search Positions - Multiple Etab", () => {
    const schoolName = `IT Positions - NEtab ${Date.now()}`
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    //////////////////////////////////
    // Create 1 head structure and 2
    // depending structures
    const chapeau = createEmptyStructure(`Chapeau - ${schoolName}`, false, session)
    const structure1 = initStructure(`1 - ${schoolName}`, session)
    const structure2 = initStructure(`2 - ${schoolName}`, session)
    attachStructureAsChild(chapeau, structure1, session)
    attachStructureAsChild(chapeau, structure2, session)

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
    //////////////////////////////////
    // Search positions

    session = authenticateWeb(adml1.login)

    let res = searchPositions('MEtab1', session);
    check(JSON.parse(res.body), {
      'adml of structure1 should see the positions of structure 1 only': pos => pos && pos.length === positions1.length,
      'all positions of structure1 should be fetched': pos => allPositionsOk(positions1, pos)
    })

    session = authenticateWeb(adml2.login)
    res = searchPositions('MEtab2', session);
    check(JSON.parse(res.body), {
      'adml of structure2 should see the positions of structure 2 only': pos => pos && pos.length === positions2.length,
      'all positions of structure1 should be fetched': pos => allPositionsOk(positions2, pos)
    })

    session = authenticateWeb(megaAdml.login)
    res = searchPositions('MEtab', session);
    check(JSON.parse(res.body), {
      'adml of structure1 and structure2 should see the positions of both structures': pos => pos && pos.length === (positions2.length + positions1.length),
      'all positions of structure1 should be fetched': pos => allPositionsOk([...positions2, ...positions1], pos)
    })
})
};
