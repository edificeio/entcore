import http from "k6/http";
import {
  getHeaders,
} from "https://raw.githubusercontent.com/edificeio/edifice-k6-commons/develop/dist/index.js";

const rootUrl = __ENV.ROOT_URL;

export function listIsolated(session, sortOn) {
  const headers = getHeaders(session);
  const params = new URLSearchParams({
    sortOn: sortOn ? sortOn : "+displayName",
    fromIndex:0,
    limitResult:50
  }).toString();

  let res = http.get(`${rootUrl}/directory/list/isolated?${params}`, undefined, {headers});
  if (res.status !== 200) {
    throw `Impossible to list isolated users with query params ?${params}`;
  }
  return JSON.parse(res.body);
}

function getUserByName(structure, firstName, lastName, session) {
  const structureUsers = http.get(`${rootUrl}/directory/user/admin/list?structureId=${structure.id}`, {
    headers: getHeaders(session),
  });
  return JSON.parse(structureUsers.body).filter(
    u => u.firstName === firstName && u.lastName === lastName
  )[0];
}

export function createUser(structure, firstName, lastName, type, session) {
  let user = getUserByName(structure, firstName, lastName, session);
  if (user) {
    console.log(`User "${firstName} ${lastName}" already exists.`);
  } else {
    const headers = getHeaders(session);
    headers["Content-Type"] = "application/x-www-form-urlencoded;charset=UTF-8";
    const payload = new URLSearchParams({
      structureId: encodeURIComponent(structure.id),
      firstName: encodeURIComponent(firstName),
      lastName: encodeURIComponent(lastName),
      type,
      birthDate: "undefined"
    }).toString();
    const res = http.post(`${rootUrl}/directory/api/user`, payload, {headers});
    if (res.status !== 200) {
      throw `Impossible to create user "${firstName} ${lastName}" of ${structure.id}`;
    }
    user = JSON.parse(res.body);
  }
  return user;
}

export function detachUserFromStructures(user, structures, session) {
    const _structures = Array.isArray(structures) ? structures : [structures];
    for (let structure of _structures) {
      http.del(
        `${rootUrl}/directory/structure/${structure.id}/unlink/${user.id}`,
        undefined,
        {headers: getHeaders(session)},
      );
    }
}

export function deleteOrPresuppressUsers(users, session) {
  const _users = Array.isArray(users) ? users : [users];
  for (let user of _users) {
    http.del(
      `${rootUrl}/directory/user?userId=${user.id}`,
      undefined,
      {headers: getHeaders(session)},
    );
  }
}
