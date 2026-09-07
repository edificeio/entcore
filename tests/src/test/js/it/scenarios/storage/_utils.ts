import { describe } from "https://jslib.k6.io/k6chaijs/4.3.4.0/index.js";
import http from "k6/http";
import { FormData } from "https://jslib.k6.io/formdata/0.0.2/index.js";

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
 * Shared fixture and the route wrappers that stay local to the storage scenarios.
 *
 * These scenarios exercise org.entcore.common.storage.impl.S3Storage through the only surface k6 can reach:
 * the workspace, directory and archive routes that call it. The workspace and archive wrappers now live in
 * edifice-k6-commons — createFolderOrFail, copyDocument, copyDocuments, copiedIds, deleteDocument,
 * deleteDocuments, getDocumentBase64, the responseType argument of downloadFile and the timeout argument of
 * verifyExportFiles. What is left here is the fixture and the two directory routes, which have no helper
 * there.
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
