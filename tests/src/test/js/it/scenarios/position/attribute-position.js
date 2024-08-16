import { check } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import {
  authenticateWeb,
  assertKo,
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
  attachStructureAsChild
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";


chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    attributePositions: {
      exec: 'testAttributePositions',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "60s",
      gracefulStop: '60s',
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
    const structure1 = initStructure(`1-${schoolName}`, session)
    const structure2 = initStructure(`2-${schoolName}`, session)
    attachStructureAsChild(chapeau, structure1, session)
    attachStructureAsChild(chapeau, structure2, session)
    ////////////////////////////////////
    // Create 1 ADML for each structure
    // and 1 ADML for the head structure
    const users1 = getUsersOfSchool(structure1, session)
    const adml1 = getRandomUserWithProfile(users1, 'Teacher');
    makeAdml(adml1, structure1, session)
    const users2 = getUsersOfSchool(structure2, session)
    const adml2 = getRandomUserWithProfile(users2, 'Teacher');
    makeAdml(adml2, structure2, session)
    const megaAdml = getRandomUserWithProfile(users2, 'Teacher', [adml2]);
    makeAdml(megaAdml, chapeau, session)
    ////////////////////////////////////
    // Create 1 position for each structure
    const position1 = createPositionOrFail(`IT - Position 1`, structure1, session);
    const position2 = createPositionOrFail(`IT - Position 2`, structure2, session);
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
        [null, "Unauthenticated"],
        [teacher1, "Non ADML (teacher)"],
        [relative1, "Non ADML (relative)"],
        [student1, "Non ADML (Student)"]]
    for(let profile of profiles) {
        const [user, label] = profile
        if(user) {
            session = authenticateWeb(teacher1.login)
        } else {
            session = null
            logout()
        }
        assertKo(attributePositions(teacher1, position1, session), `${label} should not be able to attribute a position to a teacher`);
        assertKo(attributePositions(relative1, position1, session), `${label} should not be able to attribute a position to a teacher`);
        assertKo(attributePositions(student1, position1, session), `${label} should not be able to attribute a position to a teacher`);
    }

    session = authenticateWeb(adml1.login)
    //////////////////////////////
    // Try to attribute position 
    // from another structure
    assertKo(attributePositions(teacher1, position2, session), `ADML should not be able to attribute a position from another structure to a teacher`);
    assertKo(attributePositions(relative1, position2, session), `ADML should not be able to attribute a position from another structure to a relative`);
    assertKo(attributePositions(student1, position2, session), `ADML should not be able to attribute a position from another structure to a student`);

    //////////////////////////////
    // Try to attribute position 
    // from their structure
    assertOk(attributePositions(teacher1, position1, session), `ADML should be able to attribute a position to a teacher`);
    assertKo(attributePositions(relative1, position1, session), `ADML should not be able to attribute a position to a relative`);
    assertKo(attributePositions(student1, position1, session), `ADML should not be able to attribute a position to a student`);
    

    session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    //////////////////////////////
    // Try to attribute position 
    // from another structure
    assertKo(attributePositions(teacher1, position2, session), `ADMC should not be able to attribute a position from another structure to a teacher`);
    assertKo(attributePositions(relative1, position2, session), `ADMC should not be able to attribute a position from another structure to a relative`);
    assertKo(attributePositions(student1, position2, session), `ADMC should not be able to attribute a position from another structure to a student`);

    //////////////////////////////
    // Try to attribute position 
    // from their structure
    assertOk(attributePositions(teacher2, position2, session), `ADMC should be able to attribute a position to a teacher`);
    assertKo(attributePositions(relative2, position2, session), `ADMC should not be able to attribute a position to a relative`);
    assertKo(attributePositions(student2, position2, session), `ADMC should not be able to attribute a position to a student`);

    ///////////////////////////////
    // Ensure that once a position
    // is set for a user it can be
    // retrieved
    const teacher2_2 = getRandomUserWithProfile(users2, 'Teacher', [adml2, teacher2]);
    session = authenticateWeb(adml2.login)
    attributePositions(teacher2_2, position2, session)
    let teacherUserProfile = getUserProfileOrFail(teacher2_2.id, session)
    check(teacherUserProfile, {
        'user has positions': p => !!p.positions && p.positions.length === 1,
        'user has the right position': p => p.positions[0].id === position2.id
    })
    ///////////////////////////////
    // Ensure that once a position
    // is removed it is not 
    // retrieved
    attributePositions(teacher2_2, [], session)
    teacherUserProfile = getUserProfileOrFail(teacher2_2.id, session)
    check(teacherUserProfile, {
        'user has no positions anymore after they have been removed': p => !p.positions || p.positions.length === 0
    })
})
};
