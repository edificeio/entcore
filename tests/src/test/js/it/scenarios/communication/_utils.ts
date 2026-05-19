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

export function checkTrue(res: any, name: string, actual: boolean) {
  const ok = check(res, {
    [`${name}`]: () => actual === true,
  });
  if(!ok) {
    console.error(`Was expecting ${name} to be true`);
  }
  return ok;
}

export function checkFalse(res: any, name: string, actual: boolean) {
  const ok = check(res, {
    [`${name}`]: () => actual === false,
  });
  if(!ok) {
    console.error(`Was expecting ${name} to be false`);
  }
  return ok;
}

export function checkEquals(res: any, name: string, expected: any, actual: any) {
  const ok = check(res, {
    [`${name}`]: () => expected === actual,
  });
  if(!ok) {
    console.error(`Was expecting ${name} to equal ${expected} but got ${actual}`);
  }
  return ok;
}

export function checkNotEquals(res: any, name: string, expected: any, actual: any) {
  const ok = check(res, {
    [`${name}`]: () => expected !== actual,
  });
  if(!ok) {
    console.error(`Was expecting ${name} to not equal ${expected}`);
  }
  return ok;
}

export function checkGte(res: any, name: string, expected: number, actual: number) {
  const ok = check(res, {
    [`${name}`]: () => actual >= expected,
  });
  if(!ok) {
    console.error(`Was expecting ${name} to be greater than or equal to ${expected} but got ${actual}`);
  }
  return ok;
}

export function checkStatus(res: any, name: string, expectedStatus: number) {
  return checkEquals(res, name, expectedStatus, res.status);
}

export function checkContains(res: any, name: string, array: any[], predicate: (item: any) => boolean) {
  return checkTrue(res, name, array.some(predicate));
}

export function checkNotContains(res: any, name: string, array: any[], predicate: (item: any) => boolean) {
  return checkFalse(res, name, array.some(predicate));
}

export function checkUsersCanCommunicate(user1Id: string, user2Id: string, expectedCanCommunicate: boolean) {
  const res = http.get(
    `${rootUrl}/communication/verify/${user1Id}/${user2Id}`,
    { headers: getHeaders() },
  );
  return res.status === 200 && JSON.parse(<string>res.body).canCommunicate === expectedCanCommunicate;
}