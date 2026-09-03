import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import http from "k6/http";
import { FormData } from "https://jslib.k6.io/formdata/0.0.2/index.js";
import { fail } from "k6";

import {
  BASE_URL,
  getHeaders,
  authenticateWeb,
  Session,
  Structure,
  UserInfo,
  createAndSetRole,
  linkRoleToUsers,
  createUserAndGetData,
  createEmptyStructure,
  activateUsers,
} from '../../../node_modules/edifice-k6-commons/dist/index.js';

/**
 * Shared fixture and thin route wrappers for the storage scenarios.
 *
 * These scenarios exercise org.entcore.common.storage.impl.S3Storage through the only surface k6 can reach:
 * the workspace and archive routes that call it. edifice-k6-commons covers upload, download and the archive
 * flows; the copy, delete and read-back routes have no helper there, so they are wrapped here rather than
 * inlined in every scenario.
 */

export type StorageInitData = {
  head: Structure;
  user: UserInfo;
}

/** A structure with one activated teacher holding the workspace and archive workflows. */
export function initStorageFixture(schoolName: string): StorageInitData {
  let structure: Structure | null = null;
  let user: UserInfo | null = null;
  describe("[Storage-Init] Initialize data", () => {
    <Session>authenticateWeb(__ENV.ADMC_LOGIN, __ENV.ADMC_PASSWORD);
    structure = createEmptyStructure(`${schoolName}`, true);
    user = createUserAndGetData({
      firstName: "Storage " + Date.now(),
      lastName: "User",
      "type": "Teacher",
      structureId: structure.id,
      birthDate: "2020-01-01",
      positionIds: []
    });
    activateUsers(structure);
    const roles = [
      createAndSetRole('Espace documentaire'),
      createAndSetRole('Archive'),
    ];
    const groups = [`Teachers from group ${structure.name}.`];
    for (const role of roles) {
      linkRoleToUsers(structure, role, groups);
    }
  });
  return { head: structure, user };
}

/** POST /workspace/folder — multipart form, 201 with the created folder. */
export function createFolderOrFail(name: string, parentFolderId?: string): string {
  const body: Record<string, string> = { name };
  if (parentFolderId) {
    body.parentFolderId = parentFolderId;
  }
  const res = http.post(`${BASE_URL}/workspace/folder`, body, { headers: getHeaders() });
  if (res.status !== 201) {
    fail(`could not create folder ${name}: ${res.status} - ${res.body}`);
  }
  return (res.json() as any)._id;
}

/** POST /workspace/document/copy/:id/:folder — reaches Storage.copyFile through StorageHelper. */
export function copyDocument(id: string, folderId: string) {
  return http.post(`${BASE_URL}/workspace/document/copy/${id}/${folderId}`, null, { headers: getHeaders() });
}

/** POST /workspace/documents/copy/:folder — same, in bulk. */
export function copyDocuments(ids: string[], folderId: string) {
  return http.post(`${BASE_URL}/workspace/documents/copy/${folderId}`, JSON.stringify({ ids }),
      { headers: getHeaders("application/json") });
}

/** DELETE /workspace/document/:id — reaches Storage.removeFile. */
export function deleteDocument(id: string) {
  return http.del(`${BASE_URL}/workspace/document/${id}`, null, { headers: getHeaders() });
}

/** DELETE /workspace/documents — reaches Storage.removeFiles with the whole batch. */
export function deleteDocuments(ids: string[]) {
  return http.del(`${BASE_URL}/workspace/documents`, JSON.stringify({ ids }),
      { headers: getHeaders("application/json") });
}

/** GET /workspace/document/base64/:id — reaches Storage.readFile, which buffers the whole object. */
export function getDocumentBase64(id: string) {
  return http.get(`${BASE_URL}/workspace/document/base64/${id}`, { headers: getHeaders() });
}

/**
 * GET /workspace/document/:id as raw bytes. downloadFile from the commons returns a text body, which is
 * lossy on binary content — a byte for byte comparison needs the binary response type.
 */
export function downloadDocumentBinary(id: string) {
  return http.get(`${BASE_URL}/workspace/document/${id}`,
      { headers: getHeaders(), responseType: "binary" });
}

/** The document ids of a copy response, which returns the created documents as an array. */
export function copiedIds(res: any): string[] {
  const body = res.json();
  if (!Array.isArray(body)) {
    return [];
  }
  return body.filter((d: any) => !!d && !!d._id).map((d: any) => d._id);
}

/** PUT /directory/avatar/:userId — every call goes through cleanAvatarCache, hence findByFilenameEndingWith. */
export function updateAvatar(userId: string, documentId: string) {
  return http.put(`${BASE_URL}/directory/avatar/${userId}`,
      JSON.stringify({ picture: `/workspace/document/${documentId}` }),
      { headers: getHeaders("application/json") });
}

/** GET /userbook/avatar/:id — serves the cached avatar, or redirects to the default one. */
export function getAvatar(userId: string) {
  return http.get(`${BASE_URL}/userbook/avatar/${userId}`,
      { headers: getHeaders(), redirects: 0, responseType: "binary" });
}

/**
 * POST /directory/massmessaging/column/mapping — uploads a CSV and asks for its column mapping. The service
 * pulls the uploaded directory back from storage with copyDirectoryToFs before parsing it, so a storage
 * failure surfaces as the "Failed to copy import files from storage to FS" error rather than as a mapping.
 *
 * The form fields mirror the ones edifice-k6-commons posts to /directory/wizard/import, which is a payload
 * known to pass ImportInfos.validate.
 */
export function postMassMessagingColumnMapping(structureId: string, structureName: string, csv: ArrayBuffer) {
  const form = new FormData();
  form.append("type", "CSV");
  form.append("structureName", structureName);
  form.append("structureId", structureId);
  form.append("Teacher", http.file(csv, "enseignants.csv"));
  const headers = getHeaders();
  headers["Content-Type"] = "multipart/form-data; boundary=" + form.boundary;
  return http.post(`${BASE_URL}/directory/massmessaging/column/mapping`, form.body(), { headers });
}

/**
 * GET /archive/export/verify/:exportId with an explicit, short timeout.
 *
 * The commons helper leaves k6 to its 60s default, which turns a stalled export into a poll every 61
 * seconds — silent, and long. An export that is merely not ready answers immediately; one that died
 * answers not at all, and that is worth finding out in seconds rather than in minutes.
 */
export function verifyExport(exportId: string, timeout = "10s") {
  return http.get(`${BASE_URL}/archive/export/verify/${exportId}`,
      { headers: getHeaders(), timeout });
}
