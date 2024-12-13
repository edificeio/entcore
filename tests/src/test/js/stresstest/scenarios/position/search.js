import { check } from "k6";
import http from "k6/http";
import {chai, describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import {
  authenticateWeb,
  searchPositions,
  initStructure,
  attachUserToStructures,
  getAdmlsOrMakThem,
  getOrCreatePosition,
  createEmptyStructure,
  getHeaders,
  makeAdml
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";


chai.config.logFailures = true;

const nbStructures = 500;
const nbCommonPositions = 100;
const nbUniquePositions = 20;
const rootUrl = __ENV.ROOT_URL;


export const options = {
  setupTimeout: "3h",
  maxRedirects: 0,
  scenarios: {
    searchPositions: {
      exec: 'testSearchPositions',
      executor: "constant-vus",
      vus: 1,
      duration: "1m",
      gracefulStop: '5s',
    },
  },
};

/**
 * @returns A test dataset containing
 *    - chapeau: a head structure containing the following structures
 *    - adml: ADML of the head structure
 *    - structures: a list structures depending on the head structure
 */
export function setup() {
  let chapeau;
  let adml;
  let structures;
  describe("[Position-Stress-Search] Initialize data", () => {
    let session = authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD)
    const schoolName = `Stress - Positions - Search`
    //////////////////////////////////
    // Create 1 head structure and 2
    // depending structures
    chapeau = initStructure(`Chapeau - ${schoolName}`, 'tiny')
    adml = getAdmlsOrMakThem(chapeau, 'Teacher', 1, [], session)[0]
    let res = http.get(`${rootUrl}/directory/structure/admin/list`, {
      headers: getHeaders(session),
    });
    structures = JSON.parse(res.body).filter(s => s.name.indexOf(schoolName) === 0);
    console.log(`${nbStructures - structures.length} structures to create`)
    const nbStructuresStart = structures.length;
    console.log('nbStructuresStart', nbStructuresStart)
    for(let i = nbStructuresStart; i < nbStructures; i++) {
      console.log('round', i)
      const structure = createEmptyStructure(`${schoolName}${String(i).padStart(4, '0')}`, false, session);
      attachUserToStructures(adml, [structure], session)
      makeAdml(adml, structure, session)
      for(let j = 0; j < nbCommonPositions; j++) {
        getOrCreatePosition(`Stress - Positions Search - Common - ${String(j).padStart(4, '0')}`, structure, session);
      }
      for(let j = 0; j < nbUniquePositions; j++) {
        getOrCreatePosition(`Stress - Positions Search - Unique - ${String(i).padStart(4, '0')} - ${String(j).padStart(4, '0')}`, structure, session);
      }
      structures.push(structure)
    }
    console.log('init done')
  });
  return { chapeau, adml, structures };
}

export function testSearchPositions({chapeau, adml, structures}) {
  return
  describe("[Position-Stress-Search] Search data", () => {
    let session = authenticateWeb(adml.login)
    const res = searchPositions('Positions Search', session)
    const resCheck = check(res, {
      'search call is ok': r => r.status === 200,
      'search returns all common positions': r => {
        const results = JSON.parse(r.body)
        .filter(position => position.name.indexOf('Common') > 0);
        return results.length === nbStructures * nbCommonPositions
      },
      'search returns all specific positions': r => {
        const results = JSON.parse(r.body)
        .filter(position => position.name.indexOf('Unique') > 0);
        return results.length === nbStructures * nbUniquePositions
      }
    })
    if(!resCheck) {
      console.warn(res.body)
    }
})
};
