import { check } from "k6";
import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import {
  authenticateWeb,
  triggerImport,
  getSchoolByName,
  getUsersOfSchool,
  getUserProfileOrFail,
  getPositionsOfStructure
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";
import { sleep } from 'k6';

chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    importUserWithFunctionsInAAF: {
      exec: 'testImportUserWithFunctionsInAAF',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "60s",
      gracefulStop: '5s',
    },
  },
};


/**
 * Ensure that after an AAF import :
 * - users are attached to their rightful user positions
 * - user positions n
 */
export function testImportUserWithFunctionsInAAF() {
  describe("[Position-AAFImport]", () => {
    const allPositions = [`DIRECTION / CHEF D'ETABLISSEMENT ADJOINT`, `ENSEIGNEMENT DEVANT ELEVES / ENSEIGNER EN SEGPA OU EREA`];
    const session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    console.log("Launching import....")
    triggerImport(session)
    console.log("....waiting for import to be done....")
    sleep(3)
    console.log("....stopped waiting")
    const importedStructure = getSchoolByName("CLG-CLG INTEGRATION-PARIS")
    const structurePositions = getPositionsOfStructure(importedStructure, session)
    describe("Imported structure", () => {
      check(structurePositions, {
        'all created positions are typed AAF' : positions => positions.filter(p => p.source !== 'AAF').length === 0,
        'structure has only the user positions specified in AAF': actualPositions => allPositions.length === actualPositions.length,
        'structure has all the user positions specified in AAF': actualPositions => {
          const actualNames = new Set();
          for(let p of actualPositions) {
            actualNames.add(p.name);
          }
          return allPositions.filter(expectedName => !actualNames.has(expectedName)).length === 0;
        }
      })
    })
    const users = getUsersOfSchool(importedStructure, session);
    const expectedValues = [
      ['AAAA', 'User with 2 positions', [`DIRECTION / CHEF D'ETABLISSEMENT ADJOINT`, `ENSEIGNEMENT DEVANT ELEVES / ENSEIGNER EN SEGPA OU EREA`]],
      ['BBBB', 'User with no positions', []]
    ]
    for(let expectedValue of expectedValues) {
      const [lastName, label, expectedPositionNames] = expectedValue;
      describe(label, () => {
        const user = getUserByLastName(users, lastName);
        const userPositions = getUserProfileOrFail(user.id, session).userPositions || []
        check(userPositions, {
          'user has the number of expected positions': ups => ups.length === expectedPositionNames.length,
          'user has the expected positions': ups => {
            const actualNames = new Set();
            for(let p of ups) {
              actualNames.add(p.name);
            }
            return expectedPositionNames.filter(expectedName => !actualNames.has(expectedName)).length === 0;
          }
        })
      })
    }
})
};

function getUserByLastName(users, lastName) {
  return (users || []).filter(u => u.lastName === lastName)[0]
}
