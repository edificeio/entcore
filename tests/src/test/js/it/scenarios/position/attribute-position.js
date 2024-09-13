import { check } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import {
  authenticateWeb,
  createPositionOrFail,
  getUsersOfSchool,
  initStructure,
  logout,
  attributePositions,
  getRandomUserWithProfile,
  getUserProfileOrFail,
  attachStructureAsChild,
  getAdmlsOrMakThem,
  checkReturnCode,
  switchSession,
  attachUserToStructures
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";


chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    attributePositions: {
      exec: 'testAttributePositions',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '5s',
    },
  },
};

const SEED = __ENV.SEED || ''

/**
 * @returns A test dataset containing
 *  - head: a head structure containing the following 2
 *  - headAdml: ADML of the head structure
 *  - structures: a list structures depending on the head structure
 *  - admls: a list of adml (one per structure and they are in the same order of the structures)
 *  - positions: a list of position (one per structure and they are in the same order of the structures)
 *  - multiEtabUser: a user in all structures (except head)
 */
export function setup() {
  let structureTree;
  describe("[Position-Attribute] Initialize data", () => {
    const schoolName = `IT Positions-Attribute-${SEED}`
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    //////////////////////////////////
    // Create 1 head structure and 2
    // depending structures
    const chapeau = initStructure(`Chapeau-${schoolName}`, 'tiny')
    const structure1 = initStructure(`1 - ${schoolName}`, 'tiny')
    const structure2 = initStructure(`2 - ${schoolName}`, 'tiny')
    attachStructureAsChild(chapeau, structure1, session)
    attachStructureAsChild(chapeau, structure2, session)
    ////////////////////////////////////////
    // Create ADMLs
    const megaAdml = getAdmlsOrMakThem(chapeau, 'Teacher', 1, [], session)[0]
    const adml1 = getAdmlsOrMakThem(structure1, 'Teacher', 1, [megaAdml], session)[0]
    const adml2 = getAdmlsOrMakThem(structure2, 'Teacher', 1, [megaAdml], session)[0]

    const multiEtabUser = getOrCreateMultiEtabUser([structure1, structure2], 'Teacher', [megaAdml])

    attachUserToStructures(megaAdml, [structure1, structure2], session)
    ////////////////////////////////////////
    // Create 1 ADML for each structure
    structureTree = { head: chapeau, structures: [structure1, structure2], admls: [adml1, adml2], headAdml: megaAdml}
    ////////////////////////////////////
    // Create 1 position for each structure
    const now = Date.now()
    const position1 = createPositionOrFail(`IT - Position Attribute - 1 - ${now}`, structure1, session);
    const position2 = createPositionOrFail(`IT - Position Attribute - 2 - ${now}`, structure2, session);

    cleanUsersPositions([structure1, structure2])

    structureTree = { 
        head: chapeau, 
        structures: [structure1, structure2], 
        admls: [adml1, adml2], 
        positions: [position1, position2],
        headAdml: megaAdml,
        multiEtabUser
      }
  });
  return structureTree ;
}
/**
 * Ensure that :
 * - unauthenticated users cannot attribute positions
 * - non-adml users cannot create attribute positions
 * - adml can attribute positions of the structures they administer to users they administer
 * - admc can attribute positions on every structures but only if the position and the user are in the same structure
 * @param {*} param0 Initialized data
 */
export function testAttributePositions({structures, admls, positions, headAdml, multiEtabUser }) {
  const [adml1, adml2] = admls
  const [structure1, structure2] = structures
  const [position1, position2] = positions
  describe("[Position-Attribute] Attribute positions to users", () => {
    const admcSession = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    let session;
    const users1 = getUsersOfSchool(structure1, session)
    const teacher1 = getRandomUserWithProfile(users1, 'Teacher', [adml1, headAdml, multiEtabUser]);
    const relative1 = getRandomUserWithProfile(users1, 'Relative', [adml1, headAdml, multiEtabUser]);
    const student1 = getRandomUserWithProfile(users1, 'Student', [adml1, headAdml, multiEtabUser]);
    const users2 = getUsersOfSchool(structure2, session)
    const teacher2 = getRandomUserWithProfile(users2, 'Teacher', [adml2, headAdml, multiEtabUser]);
    const relative2 = getRandomUserWithProfile(users2, 'Relative', [adml2, headAdml, multiEtabUser]);
    const student2 = getRandomUserWithProfile(users2, 'Student', [adml2, headAdml, multiEtabUser]);

    logout(session)
    
    ////////////////////////////
    // Check that non ADML users
    // cannot attribute a position to any other profile
    const profiles = [
        [null, "Unauthenticated user"],
        [teacher1, "Non ADML (teacher)"],
        [relative1, "Non ADML (relative)"],
        [student1, "Non ADML (Student)"]]
    console.log("adml 1", adml1.login)
    console.log("adml 2", adml2.login)
    console.log("adml chapeau", headAdml.login)
    console.log("teacher1 is ", teacher1.login)
    console.log("relative1 is ", relative1.login)
    console.log("student1 is ", student1.login)
    console.log("multiEtabUser", multiEtabUser.login)
    for(let profile of profiles) {
        const [user, label] = profile
        let returnCode;
        if(user) {
            session = authenticateWeb(user.login)
            returnCode = 401;
        } else {
            session = null
            logout();
            returnCode = 302
        }
        describe(`${label} attributes position to a teacher`, () => {
            let randomTeacher = getRandomUserWithProfile(users1, 'Teacher', [teacher1, adml1, headAdml, multiEtabUser]);
            console.log('randomTeacher.login', randomTeacher.login)
            tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(randomTeacher, [position1], label, 'teacher', returnCode, session, admcSession);
        })
        describe(`${label} attributes position to a relative`, () => {
            let randomRelative = getRandomUserWithProfile(users1, 'Relative', [relative1, adml1, headAdml, multiEtabUser]);
            tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(randomRelative, [position1], label, 'relative', returnCode, session, admcSession);
        })
        describe(`${label} attributes position to a student`, () => {
            let randomStudent = getRandomUserWithProfile(users1, 'Student', [student1, adml1, headAdml, multiEtabUser]);
            tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(randomStudent, [position1], label, 'student', returnCode, session, admcSession);
        })
        if (user) {
            describe(`${label} attributes position to itself`, () => {
                tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(user, [position1], label, label, 200, session, admcSession);
            })
        }
    }

    session = authenticateWeb(adml1.login)
    //////////////////////////////
    // Try to attribute position 
    // from another structure
    describe("ADML attributes an administered user a position from another structure", () => {
      tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(teacher1, [position2], 'ADML', 'teacher', 200, session, admcSession);
      tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(relative1, [position2], 'ADML', 'relative', 200, session, admcSession);
      tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(student1, [position2], 'ADML', 'student', 200, session, admcSession);

      attributePositions(teacher1, [position2], session)
      let teacherUserProfile = getUserProfileOrFail(teacher1.id, session)
      check(teacherUserProfile.userPositions, {
          'teacher loses all positions if we try to assign only a position from another structure': ps => !ps || ps.length === 0
      })
      attributePositions(teacher1, [position1, position2], session)
      teacherUserProfile = getUserProfileOrFail(teacher1.id, session)
      check(teacherUserProfile.userPositions, {
          'teacher has only the position from his/her structure': ps => ps && ps.length === 1 && ps[0].structureId === structure1.id
      })
    })
    
    describe("ADML attributes to an administered user a position of their structure", () => {
      assignNewPositionAndCheckThatItSucceeded(teacher1, [position1], 'ADML', 'teacher', session, admcSession);
      tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(relative1, [position1], 'ADML', 'relative', 200, session, admcSession);
      tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(student1, [position1], 'ADML', 'student', 200, session, admcSession);
    })
    
    describe("ADML attributes a function to another ADML in the same structure", () => {
      session = authenticateWeb(headAdml.login)
      assignNewPositionAndCheckThatItSucceeded(adml2, [position1], 'ADML', 'Other ADML', session, admcSession);
    })

    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    
    describe("ADMC attributes to a user a position from another structure", () => {
      tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(relative1, [position2], 'ADMC', 'relative', 200, session, admcSession);
      tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(student1, [position2], 'ADMC', 'student', 200, session, admcSession);

      attributePositions(teacher1, [position2], session)
      let teacherUserProfile = getUserProfileOrFail(teacher1.id, session)
      console.log('teacherUserProfile.userPositions', teacherUserProfile.userPositions)
      check(teacherUserProfile.userPositions, {
          'teacher loses all positions if we try to assign only a position from another structure': ps => !ps || ps.length === 0
      })
      attributePositions(teacher1, [position1, position2], session)
      teacherUserProfile = getUserProfileOrFail(teacher1.id, session)
      console.log('teacherUserProfile.userPositions', teacherUserProfile.userPositions)
      check(teacherUserProfile.userPositions, {
          'teacher has only the position from his/her structure': ps => ps && ps.length === 1 && ps[0].structureId === structure1.id
      })
    })
    
    describe("ADMC attributes position to a user in their structure", () => {
      assignNewPositionAndCheckThatItSucceeded(teacher2, [position2], 'ADMC', 'teacher', session, admcSession);
      tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(relative2, [position2], 'ADMC', 'relative', 200, session, admcSession);
      tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(student2, [position2], 'ADMC', 'student', 200, session, admcSession);
    })
    
    describe("2 ADML attributes position to a user in 2 different structures", () => {
      session = authenticateWeb(adml1.login)
      assignNewPositionAndCheckThatItSucceeded(multiEtabUser, [position1], 'ADML of structure 1', 'Multi Etab', session, admcSession);
      session = authenticateWeb(adml2.login)
      assignNewPositionAndCheckThatItSucceeded(multiEtabUser, [position2], 'ADML of structure 2', 'Multi Etab', session, admcSession);
      let userPositions = getUserProfileOrFail(multiEtabUser.id).userPositions;
      check(userPositions, {
        'ADML chapeau has positions of structure 1': () => containsPosition(userPositions, position1),
        'ADML chapeau has positions of structure 2': () => containsPosition(userPositions, position2),
      })
      session = authenticateWeb(adml1.login)
      attributePositions(multiEtabUser, [], session)
      userPositions = getUserProfileOrFail(multiEtabUser.id).userPositions;
      check(userPositions, {
        'ADML of a structure can remove positions of his structure on a multi etab user': () => !containsPosition(userPositions, position1),
        'ADML of a structure cannot remove positions of another structure on a multi etab user': () => containsPosition(userPositions, position2),
      })
    })
})
};

function containsPosition(actualUserPositions, positionToFind) {
  return actualUserPositions.filter(p => p.id === positionToFind.id).length > 0
}

function assignNewPositionAndCheckThatItSucceeded(user, positions, requesterType, userType, session, admcSession) {
  checkReturnCode(attributePositions(user, positions, session), `${requesterType} call to attribute a position to a ${userType} should end with 200`, 200);
  const newUserPositions = (getUserProfileOrFail(user.id, admcSession).userPositions || []);
  const newUserPositionIds = newUserPositions.map(p => p.id)
  const expectedPositionsIds = positions.map(p => p.id)
  const checks = {}
  checks[`${requesterType} should be able to attribute new positions to a ${userType}`] = () => {
    const missingPositions = expectedPositionsIds.filter(expectedPositionId => newUserPositionIds.indexOf(expectedPositionId) < 0);
    const ok = missingPositions.length === 0;
    if(!ok) {
      console.error(`------------------- ${requesterType} ----------------------`)
      console.error(`${requesterType} should have been able to attribute all positions to a ${userType}, ${missingPositions.length}/${positions.length} where not added : ${missingPositions}`)
      console.error('expectedPositionsIds', expectedPositionsIds)
      console.error('newUserPositionIds', newUserPositionIds)
      console.error(`------------------- End ----------------------`)
    }
    return ok;
  };
  return check(newUserPositions, checks);
}

function tryToAssignNewPositionAndCheckUserPositionsRemainUnchanged(user, positions, requesterType, userType, returnCode, session, admcSession) {
  switchSession(admcSession);
  const oldUserPositions = (getUserProfileOrFail(user.id, admcSession).userPositions || []);
  switchSession(session);
  checkReturnCode(attributePositions(user, positions, session), `${requesterType} call to attribute a position to a ${userType} should end with ${returnCode}`, returnCode);
  switchSession(admcSession);
  const newUserPositions = (getUserProfileOrFail(user.id, admcSession).userPositions || []);
  switchSession(session);
  const checks = {}
  checks[`${requesterType} should not be able to attribute a position to a ${userType}`] = () => {
    let ok;
    const oldUserPositionIds = oldUserPositions.map(u => u.id)
    const newUserPositionIds = newUserPositions.map(u => u.id)
    if(oldUserPositionIds.length === newUserPositionIds.length) {
      ok = newUserPositionIds.filter(newPos => oldUserPositionIds.indexOf(newPos) < 0).length === 0;
    } else {
      ok = false;
    }
    if(!ok) {
      console.error(`------------------- ${requesterType} ----------------------`)
      console.error(`${requesterType} should not be able to attribute a position to a ${userType}, expecting `, oldUserPositions, ` but got `, newUserPositions,` when adding `, positions)
      console.error('oldUserPositionIds', oldUserPositionIds)
      console.error('newUserPositionIds', newUserPositionIds)
      console.error(`------------------- End ----------------------`)
    }
    return ok;
  };
  return check(newUserPositions, checks);
}

function cleanUsersPositions(structures) {
  const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
  for(let structure of structures) {
    const users = getUsersOfSchool(structure)
    for(let user of users) {
      attributePositions(user, [], session)
    }
  }
}

function getOrCreateMultiEtabUser(structures, profile, usersToAvoid) {
  const structIds = structures.map(s => s.id)
  const forbidenUserIds = (usersToAvoid || []).map(u => u.id)
  const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
  let usersToChooseFrom = [];
  const structure = structures[0]
  const users = getUsersOfSchool(structure, session)
  for(let user of users) {
    if(user.type !== profile || forbidenUserIds.indexOf(user.id) >= 0 ) {
      continue;
    }
    const userStructs = user.structures.filter(s => structIds.indexOf(s.id) >= 0);
    if(userStructs.length === structIds.length) {
      return user;
    }
    usersToChooseFrom.push(user)
  }
  const multiEtabUser = users[0]
  const userStructs = multiEtabUser.structures.map(s => s.id)
  const structureToAttach = structures.filter(s => userStructs.indexOf(s.id) < 0)
  console.log("Attaching", multiEtabUser.login, "to etabs", structureToAttach)
  attachUserToStructures(multiEtabUser, structureToAttach, session)
  return multiEtabUser
}