import http, { AxiosResponse } from "axios";
import { ng, workspace } from "entcore";
import {
  GoogleDriveDocument,
  IGoogleDriveDocumentResponse,
  IGoogleDriveSharedFileResponse,
} from "../models/googleDriveDocument.model";
import {
  GoogleDriveQuota,
  IGoogleDriveQuotaResponse,
} from "../models/googleDriveQuota.model";
import models = workspace.v2.models;

export type GoogleDriveShareRole = "reader" | "commenter" | "writer";

export interface IGoogleDriveShareEntry {
  id: string;
  userId: string | null;
  role: GoogleDriveShareRole;
  emailAddress: string;
}

export interface IGoogleDriveShareResult {
  userId: string;
  status: "ok" | "error";
  id?: string;
  message?: string;
}

export interface IGoogleDriveWorkspaceTransferResult {
  id: string;
  status: "ok" | "error";
  message?: string;
}

export interface IGoogleDriveService {
  listDocument(userid: string, parentId?: string): Promise<Array<GoogleDriveDocument>>;

  listTrash(userid: string): Promise<Array<GoogleDriveDocument>>;

  listSharedFiles(userid: string): Promise<Array<GoogleDriveDocument>>;

  listSharedUsers(userid: string, fileId: string): Promise<Array<IGoogleDriveShareEntry>>;

  shareDocument(
    userid: string,
    fileId: string,
    targetUserId: string,
    role?: GoogleDriveShareRole,
  ): Promise<AxiosResponse>;

  shareDocuments(
    userid: string,
    fileId: string,
    targetUserIds: Array<string>,
    role?: GoogleDriveShareRole,
  ): Promise<Array<IGoogleDriveShareResult>>;

  unshareDocument(userid: string, fileId: string, targetUserId: string): Promise<AxiosResponse>;

  createFolder(userid: string, name: string, parentId?: string): Promise<AxiosResponse>;

  moveDocument(userid: string, fileId: string, parentId: string): Promise<AxiosResponse>;

  deleteDocuments(userid: string, ids: Array<string>): Promise<AxiosResponse>;

  deleteTrashDocuments(userid: string, ids: Array<string>): Promise<AxiosResponse>;

  restoreDocument(userid: string, ids: Array<string>): Promise<AxiosResponse>;

  deleteTrash(userid: string): Promise<AxiosResponse>;

  moveDocumentDriveToWorkspace(
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<Array<models.Element>>;

  copyDocumentToWorkspace(
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<Array<models.Element>>;

  moveDocumentWorkspaceToCloud(
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<AxiosResponse>;

  copyDocumentWorkspaceToCloud(
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<AxiosResponse>;

  getFile(userid: string, fileId: string, isFolder?: boolean): string;

  getFiles(userid: string, ids: Array<string>): string;

  openEditLink(userid: string, document: GoogleDriveDocument): void;

  getStorageQuota(userid: string): Promise<GoogleDriveQuota>;

  uploadLocalFilesToCloud(
    userid: string,
    files: File[],
    parentId?: string,
  ): Promise<void>;
}

// These endpoints always return 200; per-item failures only show up in the response body.
function throwOnTransferErrors(res: AxiosResponse): void {
  const results: Array<IGoogleDriveWorkspaceTransferResult> = res.data?.data ?? [];
  const failed = results.filter((r) => r.status === "error");
  if (failed.length > 0) {
    throw new Error(failed.map((f) => f.message).filter(Boolean).join("; ") || "transfer failed");
  }
}

export const googleDriveService: IGoogleDriveService = {
  listDocument: async (
    userid: string,
    parentId?: string,
  ): Promise<Array<GoogleDriveDocument>> => {
    const urlParam = parentId ? `?path=${parentId}` : "";
    return http
      .get(`/googledrive/files/user/${userid}${urlParam}`)
      .then((res: AxiosResponse) =>
        res.data.data.map((doc: IGoogleDriveDocumentResponse) =>
          new GoogleDriveDocument().build(doc),
        ),
      );
  },

  listTrash: async (userid: string): Promise<Array<GoogleDriveDocument>> => {
    return http
      .get(`/googledrive/files/user/${userid}/trash`)
      .then((res: AxiosResponse) =>
        res.data.map((doc: IGoogleDriveDocumentResponse) =>
          new GoogleDriveDocument().build(doc),
        ),
      );
  },

  listSharedFiles: async (userid: string): Promise<Array<GoogleDriveDocument>> => {
    return http
      .get(`/googledrive/files/user/${userid}/shared`)
      .then((res: AxiosResponse) =>
        res.data.data.map((doc: IGoogleDriveSharedFileResponse) =>
          new GoogleDriveDocument().buildFromShared(doc),
        ),
      )
      .then((documents: Array<GoogleDriveDocument>) => resolveOwnerDisplayNames(documents));
  },

  listSharedUsers: async (
    userid: string,
    fileId: string,
  ): Promise<Array<IGoogleDriveShareEntry>> => {
    return http
      .get(`/googledrive/files/user/${userid}/file/${encodeURIComponent(fileId)}/share`)
      .then((res: AxiosResponse) => res.data.data);
  },

  shareDocument: (
    userid: string,
    fileId: string,
    targetUserId: string,
    role: GoogleDriveShareRole = "reader",
  ): Promise<AxiosResponse> => {
    // @ts-ignore
    return http.put(
      `/googledrive/files/user/${userid}/file/${encodeURIComponent(fileId)}/share`,
      { userId: targetUserId, role },
    );
  },

  shareDocuments: (
    userid: string,
    fileId: string,
    targetUserIds: Array<string>,
    role: GoogleDriveShareRole = "reader",
  ): Promise<Array<IGoogleDriveShareResult>> => {
    // @ts-ignore
    return http
      .put(`/googledrive/files/user/${userid}/file/${encodeURIComponent(fileId)}/share/multiple`, {
        userIds: targetUserIds,
        role,
      })
      .then((res: AxiosResponse) => res.data.data as Array<IGoogleDriveShareResult>);
  },

  unshareDocument: (
    userid: string,
    fileId: string,
    targetUserId: string,
  ): Promise<AxiosResponse> => {
    // @ts-ignore
    return http.delete(
      `/googledrive/files/user/${userid}/file/${encodeURIComponent(fileId)}/share/${encodeURIComponent(targetUserId)}`,
    );
  },

  createFolder: async (
    userid: string,
    name: string,
    parentId?: string,
  ): Promise<AxiosResponse> => {
    const parentParam = parentId ? `&parentId=${parentId}` : "";
    return http.post(
      `/googledrive/files/user/${userid}/create/folder?name=${encodeURIComponent(name)}${parentParam}`,
    );
  },

  moveDocument: (
    userid: string,
    fileId: string,
    parentId: string,
  ): Promise<AxiosResponse> => {
    // @ts-ignore
    return http.put(
      `/googledrive/files/user/${userid}/move?fileId=${fileId}&parentId=${parentId}`,
    );
  },

  deleteDocuments: (
    userid: string,
    ids: Array<string>,
  ): Promise<AxiosResponse> => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    // @ts-ignore
    return http.delete(`/googledrive/files/user/${userid}/delete?${urlParams}`);
  },

  deleteTrashDocuments: (
    userid: string,
    ids: Array<string>,
  ): Promise<AxiosResponse> => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    // @ts-ignore
    return http.delete(
      `/googledrive/files/user/${userid}/trash/delete-documents?${urlParams}`,
    );
  },

  restoreDocument: (
    userid: string,
    ids: Array<string>,
  ): Promise<AxiosResponse> => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    // @ts-ignore
    return http.put(`/googledrive/files/user/${userid}/restore?${urlParams}`);
  },

  deleteTrash: (userid: string): Promise<AxiosResponse> => {
    // @ts-ignore
    return http.delete(`/googledrive/files/user/${userid}/trash/delete`);
  },

  moveDocumentDriveToWorkspace: (
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<Array<models.Element>> => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    const parentParam = parentId ? `&parentId=${parentId}` : "";
    // @ts-ignore
    return http
      .put(`/googledrive/files/user/${userid}/move/workspace?${urlParams}${parentParam}`)
      .then((res: AxiosResponse) =>
        res.data.data
          .filter((doc: any) => doc.workspace && doc.workspace._id)
          .map((doc: any) => new models.Element(doc.workspace)),
      );
  },

  copyDocumentToWorkspace: (
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<Array<models.Element>> => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    const parentParam = parentId ? `&parentId=${parentId}` : "";
    // @ts-ignore
    return http
      .put(`/googledrive/files/user/${userid}/copy/workspace?${urlParams}${parentParam}`)
      .then((res: AxiosResponse) =>
        res.data.data
          .filter((doc: any) => doc.workspace && doc.workspace._id)
          .map((doc: any) => new models.Element(doc.workspace)),
      );
  },

  moveDocumentWorkspaceToCloud: async (
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<AxiosResponse> => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    const parentParam = parentId ? `&parentId=${parentId}` : "";
    // @ts-ignore
    const res: AxiosResponse = await http.put(
      `/googledrive/files/user/${userid}/workspace/move/cloud?${urlParams}${parentParam}`,
    );
    throwOnTransferErrors(res);
    return res;
  },

  copyDocumentWorkspaceToCloud: async (
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<AxiosResponse> => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    const parentParam = parentId ? `&parentId=${parentId}` : "";
    // @ts-ignore
    const res: AxiosResponse = await http.put(
      `/googledrive/files/user/${userid}/workspace/copy/cloud?${urlParams}${parentParam}`,
    );
    throwOnTransferErrors(res);
    return res;
  },

  getFile: (userid: string, fileId: string, isFolder: boolean = false): string => {
    return `/googledrive/files/user/${userid}/file/${encodeURIComponent(fileId)}/download?isFolder=${isFolder}`;
  },

  getFiles: (userid: string, ids: Array<string>): string => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    return `/googledrive/files/user/${userid}/multiple/download?${urlParams}`;
  },

  openEditLink: (userid: string, document: GoogleDriveDocument): void => {
    window.open(
      `/googledrive/files/user/${userid}/file/${encodeURIComponent(document.id)}/edit`,
    );
  },

  getStorageQuota: async (userid: string): Promise<GoogleDriveQuota> => {
    return http
      .get(`/googledrive/files/user/${userid}/quota`)
      .then((res: AxiosResponse) =>
        new GoogleDriveQuota().build(res.data as IGoogleDriveQuotaResponse),
      );
  },

  uploadLocalFilesToCloud: async (
    userid: string,
    files: File[],
    parentId?: string,
  ): Promise<void> => {
    for (const file of files) {
      const formData = new FormData();
      formData.append("file", file, file.name);
      const uploadRes = await http.post(
        `/workspace/document?name=${encodeURIComponent(file.name)}`,
        formData,
      );
      const docId: string | undefined = uploadRes.data?._id;
      if (!docId) throw new Error(`Upload failed: no _id returned for ${file.name}`);
      const urlParams = new URLSearchParams();
      urlParams.append("id", docId);
      const parentParam = parentId ? `&parentId=${parentId}` : "";
      await http.put(
        `/googledrive/files/user/${userid}/workspace/move/cloud?${urlParams}${parentParam}`,
      );
    }
  },
};

// The owner's Google Workspace "displayName" is not reliable (it can be anything set in
// Google Admin, e.g. a raw ENT id for test accounts) — resolve the real ENT display name
// from the sharer's ENT userId instead, deduping lookups across documents by the same owner.
function resolveOwnerDisplayNames(
  documents: Array<GoogleDriveDocument>,
): Promise<Array<GoogleDriveDocument>> {
  const userIds = Array.from(
    new Set(
      documents
        .map((doc) => doc.sharedOwners?.[0]?.userId)
        .filter((id): id is string => !!id),
    ),
  );
  if (!userIds.length) return Promise.resolve(documents);

  return Promise.all(
    userIds.map((id) =>
      http
        .get(`/userbook/api/person?id=${id}`)
        .then((res: AxiosResponse) => [id, res.data?.result?.[0]?.displayName] as const)
        .catch(() => [id, undefined] as const),
    ),
  ).then((resolved) => {
    const displayNameByUserId = new Map<string, string>(
      resolved.filter(([, name]) => !!name) as Array<[string, string]>,
    );
    documents.forEach((doc) => {
      const ownerUserId = doc.sharedOwners?.[0]?.userId;
      const resolvedName = ownerUserId && displayNameByUserId.get(ownerUserId);
      if (resolvedName) doc.ownerDisplayName = resolvedName;
    });
    return documents;
  });
}

export const GoogleDriveService = ng.service(
  "GoogleDriveService",
  (): IGoogleDriveService => googleDriveService,
);
