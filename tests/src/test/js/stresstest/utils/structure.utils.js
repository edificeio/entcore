import http from 'k6/http';
import {getHeaders} from './user.utils.js';

const rootUrl = __ENV.ROOT_URL;

export function getSchoolByName(name, session) {
    let ecoles = http.get(`${rootUrl}/directory/api/ecole`, {headers: getHeaders(session)})
    const result = JSON.parse(ecoles.body).result;
    let ecoleAudience = Object.keys(result || {})
      .map(k => result[k])
      .filter(ecole => ecole.name === name) [0];
    return ecoleAudience
  }


export function getUsersOfSchool(school, session) {
    let res = http.get(`${rootUrl}/directory/structure/${school.id}/users`, { headers: getHeaders(session) })
    if( !res.status === 200) {
        throw `Impossible to get users of ${school.id}`
    }
    return JSON.parse(res.body);
}