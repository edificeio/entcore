import http from "k6/http";
import {
  assertOk,
  getHeaders,
  getUsersOfSchool,
  createUser,
} from "../../../node_modules/edifice-k6-commons/dist/index.js";

const rootUrl = __ENV.ROOT_URL;

export function listIsolated(sortOn, session) {
  const headers = getHeaders(session);
  const params = new URLSearchParams({
    sortOn: sortOn ? sortOn : "+displayName",
    fromIndex: "0",
    limitResult: "50"
  }).toString();

  let res = http.get(`${rootUrl}/directory/list/isolated?${params}`, {headers});
  assertOk(res, `Impossible to list isolated users with query params ?${params}`);
  return JSON.parse(<string>res.body);
}

function getUserByName(structure, firstName, lastName, session) {
  const users = getUsersOfSchool(structure, session)
  return users.filter(
      u => u.firstName === firstName && u.lastName === lastName
    )[0];
}

export function getOrCreateUser(structure, firstName, lastName, type, session) {
  let user = getUserByName(structure, firstName, lastName, session);
  if (user) {
    console.log(`User "${firstName} ${lastName}" already exists.`);
  } else {
    const res = createUser({
      structureId: encodeURIComponent(structure.id),
      firstName: encodeURIComponent(firstName),
      lastName: encodeURIComponent(lastName),
      type,
      birthDate: "undefined",
      positionIds: []
    }, session);
    assertOk(res, `Impossible to create user "${firstName} ${lastName}" of ${structure.id}`);
    user = JSON.parse(<string>res.body);
  }
  return user;
}

export function detachUserFromStructures(user, structures, session) {
    const _structures = Array.isArray(structures) ? structures : [structures];
    for (let structure of _structures) {
      http.del(
        `${rootUrl}/directory/structure/${structure.id}/unlink/${user.id}`,
        null,
        {headers: getHeaders(session)}
      );
    }
}

export function deleteOrPresuppressUsers(users, session) {
  const _users = Array.isArray(users) ? users : [users];
  for (let user of _users) {
    http.del(
      `${rootUrl}/directory/user?userId=${user.id}`,
      null,
      {headers: getHeaders(session)}
    );
  }
}
