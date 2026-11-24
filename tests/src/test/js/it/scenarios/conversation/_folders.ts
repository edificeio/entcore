import { getHeaders } from "../../../node_modules/edifice-k6-commons/dist/index.js";
import { check } from "k6";
import http, { RefinedResponse } from "k6/http";

const rootUrl = __ENV.ROOT_URL;

export function createFolder(payload: {
  name: string;
  parentId?: string;
}): RefinedResponse<any> {
  const headers = getHeaders();
  headers["content-type"] = "application/json";
  return http.post(`${rootUrl}/conversation/folder`, JSON.stringify(payload), {
    headers,
  });
}

export function checkCreateFolderOk(
  res: RefinedResponse<any>,
  checkName: string
) {
  const checks = {};
  checks[`${checkName} - HTTP status`] = (r) => r.status === 201;
  const ok = check(res, checks);
  if (!ok) {
    console.error(checkName, res);
  }
}

export function moveIntoFolder(intoFolderId: string, messageIds: string[]) {
  const headers = getHeaders();
  headers["content-type"] = "application/json";
  return http.put(
    `${rootUrl}/conversation/move/userfolder/${intoFolderId}`,
    JSON.stringify({ id: messageIds }),
    { headers }
  );
}
export function checkMoveIntoFolderOk(
  res: RefinedResponse<any>,
  checkName: string
) {
  const checks = {};
  checks[`${checkName} - HTTP status`] = (r) => r.status === 200;
  const ok = check(res, checks);
  if (!ok) {
    console.error(checkName, res);
  }
}

export function deleteFolder(folderId: string) {
  return http.del(`${rootUrl}/conversation/api/folders/${folderId}`);
}
