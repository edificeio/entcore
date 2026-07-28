import http, { AxiosResponse } from "axios";
import { ng, workspace } from "entcore";
import {
  GoogleDriveDocument,
  IGoogleDriveDocumentResponse,
} from "../models/googleDriveDocument.model";
import {
  GoogleDriveQuota,
  IGoogleDriveQuotaResponse,
} from "../models/googleDriveQuota.model";
import models = workspace.v2.models;

export interface IGoogleDriveService {
  listDocument(userid: string, parentId?: string): Promise<Array<GoogleDriveDocument>>;

  listTrash(userid: string): Promise<Array<GoogleDriveDocument>>;

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

  moveDocumentWorkspaceToCloud: (
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<AxiosResponse> => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    const parentParam = parentId ? `&parentId=${parentId}` : "";
    // @ts-ignore
    return http.put(
      `/googledrive/files/user/${userid}/workspace/move/cloud?${urlParams}${parentParam}`,
    );
  },

  copyDocumentWorkspaceToCloud: (
    userid: string,
    ids: Array<string>,
    parentId?: string,
  ): Promise<AxiosResponse> => {
    const urlParams = new URLSearchParams();
    ids.forEach((id) => urlParams.append("id", id));
    const parentParam = parentId ? `&parentId=${parentId}` : "";
    // @ts-ignore
    return http.put(
      `/googledrive/files/user/${userid}/workspace/copy/cloud?${urlParams}${parentParam}`,
    );
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

export const GoogleDriveService = ng.service(
  "GoogleDriveService",
  (): IGoogleDriveService => googleDriveService,
);
