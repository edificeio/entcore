import { check } from "k6";
import {
  getHeaders,
    Visible,
  } from "../../../node_modules/edifice-k6-commons/dist/index.js";
import http from "k6/http";

const rootUrl = __ENV.ROOT_URL;

export function checkSearchVisible(res, expectedUserFields:string[], expectedGroupFields:string[], checkName) {
  const checks = {}
  const visibles: Visible[] = JSON.parse(<string>res.body);
  checks[`${checkName} - HTTP status`] = (r) => r.status === 200
  checks[`${checkName} - Visible user has expected fields`] = () => {
    const visibleUser:Visible = visibles.filter(v => v.type === 'User')[0]
    return expectedUserFields.every(field => visibleUser.hasOwnProperty(field))
  }
  checks[`${checkName} - Visible group has expected fields`] = () => {
    const visibleGroup:Visible = visibles.filter(v => v.type === 'Group')[0]
    return expectedGroupFields.every(field => visibleGroup.hasOwnProperty(field))
  }
  const ok = check(res, checks);
  if(!ok) {
    console.error(checkName, res)
  }
}

export function checkUsersCanCommunicate(user1Id: string, user2Id: string, expectedCanCommunicate: boolean) {
  const res = http.get(
    `${rootUrl}/communication/verify/${user1Id}/${user2Id}`,
    { headers: getHeaders() },
  );
  return res.status === 200 && JSON.parse(<string>res.body).canCommunicate === expectedCanCommunicate;
}