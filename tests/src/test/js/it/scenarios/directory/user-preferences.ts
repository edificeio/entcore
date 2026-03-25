import { check } from "k6";
import {chai, describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import {
  authenticateWeb,
  assertOk,
  initStructure,
  attachStructureAsChild,
  checkReturnCode,
  createUser,
  getUserProfileOrFail,
  Session,
  getUsersOfSchool,
  getRandomUser,
  getUserPreferencesApi,
  setUserPreferencesApi
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    userPreferenceHomePage: {
      exec: 'testUserPreferenceHomePage',
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
  describe("[UserPreferences] Initialize data", () => {
    const schoolName = `IT Preferences`
    let session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    //////////////////////////////////
    // Create 1 head structure and 1
    // depending structures
    const chapeau = initStructure(`Chapeau-${schoolName}`, 'tiny')
    const structure1 = initStructure(`1 - ${schoolName}`, 'tiny')
    attachStructureAsChild(chapeau, structure1)
    structureTree = {
        head: chapeau, 
        structures: [structure1]
      }
  });
  return structureTree ;
}
/**
 * Ensure that homepage userPreferences work properly
 */
export function testUserPreferenceHomePage({head, structures }) {
  const [structure1] = structures
  describe("[UserPreferences-HomePage] Set enabled to different value ", () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    const headUsers = getUsersOfSchool(structure1);
    const user = getRandomUser(headUsers);
    <Session>authenticateWeb(user.login)

    const setBetaEnabled = setUserPreferencesApi({ homePage: {betaEnabled : true}});
    check( setBetaEnabled, {
      "Response status should be successfull": ps => ps.status < 300
    });
    //retrieve homepage preference to verify the value
    let userPreferencesResponse = getUserPreferencesApi();

    check( userPreferencesResponse, {
      "Homepage preferences should not be null": up => up.homePage !== null,
      "Homepage preferences should contain betaEnabled = true ": up => up.homePage.betaEnabled,
    });

    const setBetaDisabled = setUserPreferencesApi({ homePage: {betaEnabled : false}});
    check( setBetaDisabled, {
      "Response status should be successfull": ps => ps.status < 300
    });
    //retrieve homepage preference to verify the value
    userPreferencesResponse = getUserPreferencesApi();

    check( userPreferencesResponse, {
      "Homepage preferences should not be null": up => up.homePage !== null,
      "Homepage preferences should contain betaEnabled = false ": up => !up.homePage.betaEnabled,
    });

    const resetPreference = setUserPreferencesApi({ homePage : null});
    check( resetPreference, {
      "Response status should be successfull": ps => ps.status < 300
    });
    //retrieve homepage preference to verify the value
    userPreferencesResponse = getUserPreferencesApi();

    check( userPreferencesResponse, {
      "Homepage preferences should be null": up => up.homePage === null
    });
})
}
