import chai, { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.2/index.js";
import {
  authenticateWeb,
  createStructure,
  importCSVToStructure,
  getUsersOfSchool
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";
import { checkUserAndPositions, checkStructureHasOnlyThesePositions } from './_utils.js';

chai.config.logFailures = true;

export const options = {
  setupTimeout: "1h",
  maxRedirects: 0,
  thresholds: {
    checks: ["rate == 1.00"],
  },
  scenarios: {
    userPositionsFromCSVImport: {
      exec: 'testUserPositionsFromCSVImport',
      executor: "per-vu-iterations",
      vus: 1,
      maxDuration: "5s",
      gracefulStop: '5s',
    },
  },
};

const seed = Date.now()
const dataRootPath = __ENV.DATA_ROOT_PATH;
const teacherDataBefore = open(`${dataRootPath}/positions/csv/before/enseignants.csv`, 'b')
const teacherDataAfter = open(`${dataRootPath}/positions/csv/after/enseignants.csv`, 'b')


export function testUserPositionsFromCSVImport() {
    describe("[Position-CSV]", () => {
        let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
        const schoolName = `IT - Positions - CSV - ${seed}`
        const structure = createStructure(schoolName, teacherDataBefore, session)
        describe("First import", () => {
            checkStructureHasOnlyThesePositions(structure, [
                'DIRECTION / CHEF D\'ETABLISSEMENT ADJOINT',
                'Fonction qui ne sera plus attribuée',
                'Fonction qui restera attribuée'
            ], session)
            const users = getUsersOfSchool(structure, session);
            const expectedValues = [
              ['A000', 'User with AAF position', ['DIRECTION / CHEF D\'ETABLISSEMENT ADJOINT']],
              ['A001', 'User with no position', []],
              ['A002', 'User with one position that will be lost', ['Fonction qui ne sera plus attribuée']],
              ['A003', 'User with one position that will stay', ['Fonction qui restera attribuée']]
            ]
            checkUserAndPositions(expectedValues, users, session)
        })
        describe("Second import", () => {
            importCSVToStructure(structure, teacherDataAfter, {}, session)
            checkStructureHasOnlyThesePositions(structure, [
                'Ma nouvelle fonction',
                'Fonction qui restera attribuée'
            ], session)
            const users = getUsersOfSchool(structure, session);
            const expectedValues = [
              ['A000', 'User with no positions anymore', []],
              ['A001', 'User with a position which stayed', ['Fonction qui restera attribuée']],
              ['A002', 'User with new position', ['Ma nouvelle fonction']],
              ['A003', 'User with no positions att all', []],
              ['A004', 'New user with no positions att all', []]
            ]
            checkUserAndPositions(expectedValues, users, session)
        })
    });
}

