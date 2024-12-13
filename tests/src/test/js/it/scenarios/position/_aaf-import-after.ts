import { check } from "k6";
import {chai, describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import {
  authenticateWeb,
  triggerImport,
  getSchoolByName,
  getUsersOfSchool,
  getPositionsOfStructure,
  attributePositions,
  deletePosition,
  getOrCreatePosition,
  Session
} from "../../../node_modules/edifice-k6-commons/dist/index.js";
import { sleep } from 'k6';
import {
  checkUserAndPositions,
  getUserByLastName
} from './_utils.ts';

chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    importUserWithFunctionsInAAFStep2: {
      exec: 'testImportUserWithFunctionsInAAFStep2',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "60s",
      gracefulStop: '5s',
    },
  },
};

let positionsToBeTornDown = new Set();


/**
 * Ensure that after an AAF import :
 * - users are attached to their rightful user positions
 * - user positions that are not present anymore in the file are removed from the structure
 */
export function testImportUserWithFunctionsInAAFStep2() {
  describe("[Position-AAFImport]", () => {
    const allPositions = [
      /*`DIRECTION / CHEF D'ETABLISSEMENT ADJOINT`,*/ //this one has disappeared
      `ACCOMPAGNEMENT / ACCOMPAGNEMENT ELEVES SITUATION HANDICAP`, // <-- already existed
      // Below are the new positions
      `PRESTATIONS / ENTRETIEN`,
      `ARTICHAUT / PATATES`,
      `LEGUMES / HARICOTS`,
      `LEGUMES / PETITS POIS`
    ];
    const session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const importedStructure = getSchoolByName("CLG-CLG INTEGRATION-PARIS", session)
    const users = getUsersOfSchool(importedStructure, session);
    

    const manualPosition = getOrCreatePosition(`IT Position - Manual`, importedStructure, session)
    let user = getUserByLastName(users, 'AAAA');
    attributePositions(user, [manualPosition], session)
    

    console.log("Launching import....")
    triggerImport(session)
    console.log("....waiting for import to be done....")
    sleep(3)
    console.log("....stopped waiting")
    
    const structurePositions = getPositionsOfStructure(importedStructure, session)
    describe("Imported structure", () => {
      check(structurePositions, {
        'manual user position remains after an AAF import' : positions => positions.filter(p => p.source !== 'AAF').length === 1,
        'structure has only the user positions specified in AAF and the manual one': actualPositions => allPositions.length === (actualPositions.length - 1),
        'structure has all the user positions specified in AAF': actualPositions => {
          const actualNames = new Set();
          for(let p of actualPositions) {
            actualNames.add(p.name);
          }
          return allPositions.filter(expectedName => !actualNames.has(expectedName)).length === 0;
        }
      })
    })
    const expectedValues = [
      ['AAAA', 'User with 2 AAF positions and 1 manual', [`PRESTATIONS / ENTRETIEN`, `ACCOMPAGNEMENT / ACCOMPAGNEMENT ELEVES SITUATION HANDICAP`, `IT Position - Manual`]],
      ['BBBB', 'User who didn\'t have positions before', []],
      ['CCCC', 'New user with multiple positions', [`ARTICHAUT / PATATES`, `LEGUMES / HARICOTS`, `LEGUMES / PETITS POIS`]]
    ]
    checkUserAndPositions(expectedValues, users, session)
})
};

export function teardown() {
    const session = <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    // Removing all positions from structure, for subsequent test runs
    const structurePositions = getPositionsOfStructure(getSchoolByName("CLG-CLG INTEGRATION-PARIS", session), session)
    for (let p of structurePositions) {
      deletePosition(p.id, session)
    }
}