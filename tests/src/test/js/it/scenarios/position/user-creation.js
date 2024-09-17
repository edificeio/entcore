import { check } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import {
  authenticateWeb,
  getOrCreatePosition,
  assertOk,
  initStructure,
  attachStructureAsChild,
  getAdmlsOrMakThem,
  checkReturnCode,
  attachUserToStructures,
  createUser,
  getUserProfileOrFail
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";

chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    createUserWithPositions: {
      exec: 'testCreateUserWithPositions',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '5s',
    },
  },
};

const seed = Date.now()

export function setup() {
  let structureTree;
  describe("[Position-UserCreation] Initialize data", () => {
    const schoolName = `IT Positions-Creation`
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

    attachUserToStructures(megaAdml, [structure1, structure2], session)
    ////////////////////////////////////////
    // Create 1 ADML for each structure
    structureTree = { head: chapeau, structures: [structure1, structure2], admls: [adml1, adml2], headAdml: megaAdml}
    const positions = [0, 1, 2, 3].map(i => getOrCreatePosition(`${schoolName} - Position ${i}`, structure1, session))
    structureTree = { 
        head: chapeau, 
        structures: [structure1, structure2], 
        admls: [adml1, adml2],
        headAdml: megaAdml,
        positions
      }
  });
  return structureTree ;
}
/**
 * Ensure that only Personnel users can be created with user positions
 */
export function testCreateUserWithPositions({structures, admls, positions, headAdml }) {
  const [adml1, adml2] = admls
  const [structure1, structure2] = structures
  describe("[Position-UserCreation] Attribute positions to users on creation", () => {
    const session = authenticateWeb(adml1.login)
    const profilesThatCannotBeCreatedWithPositions = ['Student', 'Relative', 'Teacher']
    for(let profile of profilesThatCannotBeCreatedWithPositions) {
        describe(`Creation of a ${profile} should not be possible with a position`, () => {
            const userCreationRequest = createUserCreationRequest(profile, structure1, positions)
            let res = createUser(userCreationRequest, structure1, session);
            checkReturnCode(res, `should not be able to create a ${profile} with positions`, 403)
        })
    }
    const profilesThatCanBeCreatedWithPositions = ['Personnel']
    for(let profile of profilesThatCanBeCreatedWithPositions) {
        describe(`Creation of a ${profile} should be possible with a position`, () => {
          const userCreationRequest = createUserCreationRequest(profile, structure1, positions)
          let res = createUser(userCreationRequest, structure1, session);
          assertOk(res, `should be able to create a ${profile} with positions`)
          const user = JSON.parse(res.body)
          const newUserPositions = (getUserProfileOrFail(user.id, session).userPositions || []);
          const actualPositionIds = newUserPositions.map(p => p.id)
          const checkOk = check(newUserPositions, {
            "should have all positions specified at creation and only them": ps => ps.length === positions.length &&
                                                                      positions.filter(p => actualPositionIds.indexOf(p.id)<0).length === 0
          })
          if(!checkOk) {
            console.warn("Error ", newUserPositions, "instead of ", positions)
            console.warn("Could not find", positions.filter(p => actualPositionIds.indexOf(p.id)<0))
          }
        })
    }
})
};

function createUserCreationRequest(profile, structure, positions) {
  return {
    firstName: `Pr ${profile.substring(0, 1)} ${seed}`,
    lastName: `Nom ${profile.substring(0, 1)} ${seed}`,
    type: profile,
    structureId: structure.id,
    birthDate: "2010-10-10",
    positionIds: positions.map(p => p.id)
  }
}