import http, { AxiosResponse } from "axios";
import { ng, workspace } from "entcore";
import {
  IDocumentResponse,
  SyncDocument,
} from "../models/nextcloudFolder.model";
import models = workspace.v2.models;

export interface INextcloudService {
  openNextcloudLink(document: SyncDocument, nextcloudUrl: string): void;

  openNextcloudEditLink(document: SyncDocument, nextcloudUrl: string): void;

  getNextcloudUrl(): Promise<string>;

  getIsNextcloudUrlHidden(): Promise<boolean>;

  listDocument(userid: string, path?: string): Promise<Array<SyncDocument>>;

  uploadDocuments(
    userid: string,
    files: Array<File>,
    path?: string,
  ): Promise<AxiosResponse>;

  moveDocument(
    userid: string,
    path: string,
    destPath: string,
  ): Promise<AxiosResponse>;

  moveDocumentNextcloudToWorkspace(
    userid: string,
    paths: Array<string>,
    parentId?: string,
  ): Promise<AxiosResponse>;

  moveDocumentWorkspaceToCloud(
    userid: string,
    ids: Array<string>,
    cloudDocumentName?: string,
  ): Promise<AxiosResponse>;

  copyDocumentToWorkspace(
    userid: string,
    paths: Array<string>,
    parentId?: string,
  ): Promise<Array<models.Element>>;

  deleteDocuments(userid: string, path: Array<string>): Promise<AxiosResponse>;

  deleteTrashDocuments(
    userid: string,
    path: Array<string>,
  ): Promise<AxiosResponse>;

  restoreDocument(userid: string, paths: Array<string>): Promise<AxiosResponse>;

  deleteTrash(userid: string): Promise<AxiosResponse>;

  getFile(
    userid: string,
    fileName: string,
    path: string,
    contentType: string,
    isFolder?: boolean,
  ): string;

  getFiles(userid: string, path: string, files: Array<string>): string;

  createFolder(userid: string, folderPath: String): Promise<AxiosResponse>;

  listTrash(userid: string): Promise<Array<SyncDocument>>;
}

export const nextcloudService: INextcloudService = {
  openNextcloudLink: (document: SyncDocument, nextcloudUrl: string): void => {
    const url: string = document.path.includes("/")
      ? document.path.substring(0, document.path.lastIndexOf("/"))
      : "";
    const dir: string = url ? url : "/";
    window.open(
      `${nextcloudUrl}/index.php/apps/files?dir=${dir}&openfile=${document.fileId}`,
    );
  },

  openNextcloudEditLink: (
    document: SyncDocument,
    nextcloudUrl: string,
  ): void => {
    const url: string = `${nextcloudUrl}/index.php/apps/onlyoffice/${document.fileId}`;
    window.open(url);
  },

  getNextcloudUrl: async (): Promise<string> => {
    return http
      .get(`/nextcloud/config/url`)
      .then((res: AxiosResponse) => res.data.url);
  },

  getIsNextcloudUrlHidden: async (): Promise<boolean> => {
    return http
      .get(`/nextcloud/config/isNextcloudUrlHidden`)
      .then((res: AxiosResponse) => res.data.isNextcloudUrlHidden);
  },

  createFolder: async (
    userid: string,
    folderPath: String,
  ): Promise<AxiosResponse> => {
    const urlParam: string = folderPath ? `?path=${folderPath}` : "";
    return http.post(
      `/nextcloud/files/user/${userid}/create/folder${urlParam}`,
    );
  },

  listDocument: async (
    userid: string,
    path?: string,
  ): Promise<Array<SyncDocument>> => {
    const urlParam: string = path ? `?path=${path}` : "";
    return http
      .get(`/nextcloud/files/user/${userid}${urlParam}`)
      .then((res: AxiosResponse) =>
        res.data.data.map((document: IDocumentResponse) =>
          new SyncDocument().build(document),
        ),
      );
  },

  uploadDocuments(
    userid: string,
    files: Array<File>,
    path?: string,
  ): Promise<AxiosResponse> {
    const urlParam: string = path ? `?path=${path}` : "";
    const formData: FormData = new FormData();
    const headers = {
      headers: {
        "Content-type": "multipart/form-data",
        "File-Count": files.length,
      },
    };
    files.forEach((file) => {
      formData.append("fileToUpload[]", file);
    });
    // @ts-ignore
    return http.put(
      `/nextcloud/files/user/${userid}/upload${urlParam}`,
      formData,
      headers,
    );
  },

  moveDocument: (
    userid: string,
    path: string,
    destPath: string,
  ): Promise<AxiosResponse> => {
    const urlParam: string = `?path=${path}&destPath=${destPath}`;
    // @ts-ignore
    return http.put(`/nextcloud/files/user/${userid}/move${urlParam}`);
  },

  moveDocumentNextcloudToWorkspace: (
    userid: string,
    paths: Array<string>,
    parentId?: string,
  ): Promise<AxiosResponse> => {
    let urlParams: URLSearchParams = new URLSearchParams();
    paths.forEach((path: string) => urlParams.append("path", path));
    const parentIdParam: string = parentId ? `&parentId=${parentId}` : "";
    // @ts-ignore
    return http
      .put(
        `/nextcloud/files/user/${userid}/move/workspace?${urlParams}${parentIdParam}`,
      )
      .then((res: AxiosResponse) =>
        res.data.data
          .filter((document) => document._id)
          .map((document) => new models.Element(document)),
      );
  },

  moveDocumentWorkspaceToCloud: (
    userid: string,
    ids: Array<string>,
    cloudDocumentName?: string,
  ): Promise<AxiosResponse> => {
    let urlParams: URLSearchParams = new URLSearchParams();
    ids.forEach((path: string) => urlParams.append("id", path));
    const parentDocumentNameParam: string = cloudDocumentName
      ? `&parentName=${cloudDocumentName}`
      : "";
    // @ts-ignore
    return http.put(
      `/nextcloud/files/user/${userid}/workspace/move/cloud?${urlParams}${parentDocumentNameParam}`,
    );
  },

  copyDocumentToWorkspace(
    userid: string,
    paths: Array<string>,
    parentId?: string,
  ): Promise<Array<models.Element>> {
    let urlParams: URLSearchParams = new URLSearchParams();
    paths.forEach((path: string) => urlParams.append("path", path));
    const parentIdParam: string = parentId ? `&parentId=${parentId}` : "";
    // @ts-ignore
    return http
      .put(
        `/nextcloud/files/user/${userid}/copy/workspace?${urlParams}${parentIdParam}`,
      )
      .then((res: AxiosResponse) =>
        res.data.data
          .filter((document) => document._id)
          .map((document) => new models.Element(document)),
      );
  },

  deleteDocuments(
    userid: string,
    paths: Array<string>,
  ): Promise<AxiosResponse> {
    let urlParams: URLSearchParams = new URLSearchParams();
    paths.forEach((path: string) => {
      urlParams.append("path", path);
    });
    // @ts-ignore
    return http.delete(`/nextcloud/files/user/${userid}/delete?${urlParams}`);
  },

  restoreDocument: (
    userid: string,
    paths: Array<string>,
  ): Promise<AxiosResponse> => {
    let urlParams: URLSearchParams = new URLSearchParams();
    paths.forEach((path: string) => {
      urlParams.append("path", path);
    });
    // @ts-ignore
    return http.put(`/nextcloud/files/user/${userid}/restore?${urlParams}`);
  },

  listTrash: async (userid: string): Promise<Array<SyncDocument>> => {
    return http
      .get(`/nextcloud/files/user/${userid}/trash`)
      .then((res: AxiosResponse) =>
        res.data.map((document: IDocumentResponse) =>
          new SyncDocument().build(document),
        ),
      );
  },

  deleteTrash(userid: string): Promise<AxiosResponse> {
    // @ts-ignore
    return http.delete(`/nextcloud/files/user/${userid}/trash/delete`);
  },

  deleteTrashDocuments(
    userid: string,
    paths: Array<string>,
  ): Promise<AxiosResponse> {
    let urlParams: URLSearchParams = new URLSearchParams();
    paths.forEach((path: string) => {
      urlParams.append("path", path);
    });
    // @ts-ignore
    return http.delete(
      `/nextcloud/files/user/${userid}/trash/delete-documents?${urlParams}`,
    );
  },

  getFile: (
    userid: string,
    fileName: string,
    path: string,
    contentType: string,
    isFolder: boolean = false,
  ): string => {
    const pathParam: string = path ? `?path=${path}` : "";
    const contentTypeParam: string =
      path && contentType ? `&contentType=${contentType}` : "";
    const isFolderParam: string = pathParam
      ? `&isFolder=${isFolder}`
      : `?isFolder=${isFolder}`;
    const urlParam: string = `${pathParam}${contentTypeParam}${isFolderParam}`;
    return `/nextcloud/files/user/${userid}/file/${encodeURI(
      fileName,
    )}/download${urlParam}`;
  },

  getFiles: (userid: string, path: string, files: Array<string>): string => {
    const pathParam: string = `?path=${path}`;
    let filesParam: string = "";
    files.forEach((file: string) => {
      filesParam += `&file=${file}`;
    });
    const urlParam: string = `${pathParam}${filesParam}`;
    return `/nextcloud/files/user/${userid}/multiple/download${urlParam}`;
  },
};

export const NextcloudService = ng.service(
  "NextcloudService",
  (): INextcloudService => nextcloudService,
);
