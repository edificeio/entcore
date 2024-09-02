import { check } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import {
  authenticateWeb,
  assertOk,
  makeAdml,
  createPositionOrFail,
  getUsersOfSchool,
  createEmptyStructure,
  initStructure,
  logout,
  attributePositions,
  getRandomUserWithProfile,
  getUserProfileOrFail,
  attachStructureAsChild,
  getAdmlsOrMakThem,
  checkReturnCode
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
 */
export function setup() {
  let structureTree;
  describe("[Position-Attribute] Initialize data", () => {
    const schoolName = `IT Positions-Attribute-${SEED}`
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    //////////////////////////////////
    // Create 1 head structure and 2
    // depending structures
    const chapeau = createEmptyStructure(`Chapeau-${schoolName}`, false, session)
    const structure1 = initStructure(`1 - ${schoolName}`, 'tiny')
    const structure2 = initStructure(`2 - ${schoolName}`, 'tiny')
    const structure3 = initStructure(`3 - ${schoolName}`, 'tiny')
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
    ////////////////////////////////////
    // Create 1 position for each structure
    const now = Date.now()
    const position1 = createPositionOrFail(`IT - Position Attribute - 1 - ${now}`, structure1, session);
    const position2 = createPositionOrFail(`IT - Position Attribute - 2 - ${now}`, structure2, session);
    structureTree = { 
        head: chapeau, 
        structures: [structure1, structure2], 
        admls: [adml1, adml2], 
        positions: [position1, position2],
        headAdml: megaAdml}
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
export function testAttributePositions({structures, admls, positions, headAdml }) {
  const [adml1, adml2] = admls
  const [structure1, structure2] = structures
  const [position1, position2] = positions
  describe("[Position-Attribute] Attribute positions to users", () => {
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const users1 = getUsersOfSchool(structure1, session)
    const teacher1 = getRandomUserWithProfile(users1, 'Teacher', [adml1, headAdml]);
    const relative1 = getRandomUserWithProfile(users1, 'Relative', [adml1, headAdml]);
    const student1 = getRandomUserWithProfile(users1, 'Student', [adml1, headAdml]);
    const users2 = getUsersOfSchool(structure2, session)
    const teacher2 = getRandomUserWithProfile(users2, 'Teacher', [adml2]);
    const relative2 = getRandomUserWithProfile(users2, 'Relative', [adml2]);
    const student2 = getRandomUserWithProfile(users2, 'Student', [adml2]);

    logout(session)
    
    ////////////////////////////
    // Check that non ADML users
    // cannot attribute a position to a teacher
    const profiles = [
        [null, "Unauthenticated user"],
        [teacher1, "Non ADML (teacher)"],
        [relative1, "Non ADML (relative)"],
        [student1, "Non ADML (Student)"]]
    console.log("teacher1 is ", teacher1.login)
    console.log("relative1 is ", relative1.login)
    console.log("student1 is ", student1.login)
    for(let profile of profiles) {
        const [user, label] = profile
        let returnCode;
        if(user) {
            session = authenticateWeb(teacher1.login)
            returnCode = 401;
        } else {
            session = null
            logout();
            returnCode = 302
        }
        describe(`${label} attributes position to a teacher`, () => {
          checkReturnCode(attributePositions(teacher1, [position1], session), `${label} should not be able to attribute a position to a teacher`, returnCode);
          checkReturnCode(attributePositions(relative1, [position1], session), `${label} should not be able to attribute a position to a relative`, returnCode);
          checkReturnCode(attributePositions(student1, [position1], session), `${label} should not be able to attribute a position to a student`, returnCode);
        })
    }

    session = authenticateWeb(adml1.login)
    //////////////////////////////
    // Try to attribute position 
    // from another structure
    describe("ADML attributes position to a user in another structure", () => {
      checkReturnCode(attributePositions(teacher1, [position2], session), `ADML should not be able to attribute a position from another structure to a teacher`, 401);
      checkReturnCode(attributePositions(relative1, [position2], session), `ADML should not be able to attribute a position from another structure to a relative`, 401);
      checkReturnCode(attributePositions(student1, [position2], session), `ADML should not be able to attribute a position from another structure to a student`, 401);
    })
    //////////////////////////////
    // Try to attribute position 
    // from their structure
    describe("ADML attributes position to a user in the administered structure", () => {
      assertOk(attributePositions(teacher1, [position1], session), `ADML should be able to attribute a position to a teacher`);
      checkReturnCode(attributePositions(relative1, [position1], session), `ADML should not be able to attribute a position to a relative`, 401);
      checkReturnCode(attributePositions(student1, [position1], session), `ADML should not be able to attribute a position to a student`, 401);
    })

    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    
    describe("ADMC attributes position to a user in another structure", () => {
      //////////////////////////////
      // Try to attribute position 
      // from another structure
      checkReturnCode(attributePositions(teacher1, [position2], session), `ADMC should not be able to attribute a position from another structure to a teacher`, 401);
      checkReturnCode(attributePositions(relative1, [position2], session), `ADMC should not be able to attribute a position from another structure to a relative`, 401);
      checkReturnCode(attributePositions(student1, [position2], session), `ADMC should not be able to attribute a position from another structure to a student`, 401);

      //////////////////////////////
      // Try to attribute position 
      // from their structure
      assertOk(attributePositions(teacher2, [position2], session), `ADMC should be able to attribute a position to a teacher`);
      checkReturnCode(attributePositions(relative2, [position2], session), `ADMC should not be able to attribute a position to a relative`, 401);
      checkReturnCode(attributePositions(student2, [position2], session), `ADMC should not be able to attribute a position to a student`, 401);
    })

    describe("Ability to retrieve a user position", () => {
      ///////////////////////////////
      // Ensure that once a position
      // is set for a user it can be
      // retrieved
      const teacher2_2 = getRandomUserWithProfile(users2, 'Teacher', [adml2, teacher2]);
      session = authenticateWeb(adml2.login)
      attributePositions(teacher2_2, [position2], session)
      let teacherUserProfile = getUserProfileOrFail(teacher2_2.id, session)
      check(teacherUserProfile, {
          'user has positions': p => !!p.userPositions && p.userPositions.length === 1,
          'user has the right position': p => p.userPositions[0].id === position2.id
      })
      ///////////////////////////////
      // Ensure that once a position
      // is removed it is not 
      // retrieved
      attributePositions(teacher2_2, [], session)
      teacherUserProfile = getUserProfileOrFail(teacher2_2.id, session)
      check(teacherUserProfile, {
          'user has no positions anymore after they have been removed': p => !p.userPositions || p.userPositions.length === 0
      })
    })
})
};
