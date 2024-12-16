import { check } from "k6";
import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import {
  getUserProfileOrFail,
  getPositionsOfStructure
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

export function checkUserAndPositions(expectedValues, users, session) {
    for(let expectedValue of expectedValues) {
        const [lastName, label, expectedPositionNames] = expectedValue;
        describe(label, () => {
          const user = getUserByLastName(users, lastName);
          const userPositions = getUserProfileOrFail(user.id, session).userPositions || []
          check(userPositions, {
            'user has the number of expected positions': ups => ups.length === expectedPositionNames.length,
            'user has the expected positions': ups => {
              const actualNames = getSetOfNames(userPositions)
              return expectedPositionNames.filter(expectedName => !actualNames.has(expectedName)).length === 0;
            }
          })
        })
    }
}

export function getUserByLastName(users, lastName) {
  return (users || []).filter(u => u.lastName === lastName)[0]
}

export function checkStructureHasOnlyThesePositions(structure, positionNames, session) {
  const positions = getPositionsOfStructure(structure, session);
  return check(positions, {
    'structure only has expected positions': actualPositions => {
      let ok = actualPositions.length === positionNames.length;
      const actualNames = getSetOfNames(actualPositions)
      ok = ok && positionNames.filter(expected => !actualNames.has(expected)).length === 0
      if(!ok) {
        console.warn("Expecting", positionNames, "but got", positions)
      }
      return ok;
    }
  })
}

function getSetOfNames(positions) {
  const names = new Set();
  for(let p of positions) {
    names.add(p.name);
  }
  return names;
}