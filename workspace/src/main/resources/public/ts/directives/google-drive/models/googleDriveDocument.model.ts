import { idiom as lang, model, workspace } from "entcore";
import { DocumentRole } from "../enums/documentRole.enum";
import { DocumentsType } from "../enums/documentsType.enum";
import models = workspace.v2.models;

const GOOGLE_FOLDER_MIME = "application/vnd.google-apps.folder";
const GOOGLE_DOC_MIME = "application/vnd.google-apps.document";
const GOOGLE_SHEET_MIME = "application/vnd.google-apps.spreadsheet";
const GOOGLE_SLIDE_MIME = "application/vnd.google-apps.presentation";

export interface IGoogleDriveDocumentResponse {
  id: string;
  name: string;
  mimeType: string;
  size: number;
  modifiedTime: string;
  shared?: boolean;
}

export interface IGoogleDriveSharedOwner {
  userId: string | null;
  displayName: string;
  emailAddress: string;
}

export interface IGoogleDriveSharedFileResponse {
  id: string;
  name: string;
  mimeType: string;
  size: number;
  modifiedTime: string;
  sharedWithMeTime: string;
  role: "reader" | "commenter" | "writer";
  owners: Array<IGoogleDriveSharedOwner>;
}

export class GoogleDriveDocument {
  id: string;
  name: string;
  mimeType: string;
  size: number;
  modifiedTime: string;
  isFolder: boolean;
  role: DocumentRole;
  type: DocumentsType;
  editable: boolean;
  children: Array<GoogleDriveDocument>;
  ownerDisplayName: string;
  cacheChildren: models.CacheList<any>;
  cacheDocument: models.CacheList<any>;

  selected?: boolean;
  isGoogleDriveParent?: boolean;
  isStaticFolder?: boolean;
  staticFolderType?: "trashbin" | "shared";

  // Grouping node wrapping Mon Drive/Documents partagés/Corbeille; openDocument() no-ops for it.
  isRootGroup?: boolean;

  // True when the current user (owner) has shared this document with at least one other user.
  isShared?: boolean;

  // Set only for documents shared with the current user (fetched via listSharedFiles).
  // Named "permissionRole" (not "role") because `role` already holds the DocumentRole
  // icon type (folder/doc/xls/...), unrelated to Drive sharing permission levels.
  sharedWithMeTime?: string;
  permissionRole?: "reader" | "commenter" | "writer";
  sharedOwners?: Array<IGoogleDriveSharedOwner>;

  build(data: IGoogleDriveDocumentResponse): GoogleDriveDocument {
    this.id = data.id;
    this.name = data.name;
    this.mimeType = data.mimeType || "";
    this.size = data.size;
    this.modifiedTime = data.modifiedTime;
    this.isShared = !!data.shared;
    this.isFolder = this.mimeType === GOOGLE_FOLDER_MIME;
    this.ownerDisplayName = model.me.login;
    this.type = this.isFolder ? DocumentsType.FOLDER : DocumentsType.FILE;
    this.role = this.determineRole();
    this.editable = this.isEditable();
    this.children = [];
    this.cacheChildren = new models.CacheList<any>(0, () => false, () => false);
    this.cacheChildren.setData([]);
    this.cacheChildren.disableCache();
    this.cacheDocument = new models.CacheList<any>(0, () => false, () => false);
    this.cacheDocument.setData([]);
    this.cacheDocument.disableCache();
    return this;
  }

  determineRole(): DocumentRole {
    if (this.isFolder) return DocumentRole.FOLDER;
    switch (this.mimeType) {
      case GOOGLE_DOC_MIME: return DocumentRole.DOC;
      case GOOGLE_SHEET_MIME: return DocumentRole.XLS;
      case GOOGLE_SLIDE_MIME: return DocumentRole.PPT;
    }
    if (this.mimeType.includes("pdf")) return DocumentRole.PDF;
    if (this.mimeType.includes("spreadsheet") || this.mimeType.includes("excel")) return DocumentRole.XLS;
    if (this.mimeType.includes("presentation") || this.mimeType.includes("powerpoint")) return DocumentRole.PPT;
    if (this.mimeType.includes("image")) return DocumentRole.IMG;
    if (this.mimeType.includes("video")) return DocumentRole.VIDEO;
    if (this.mimeType.includes("audio")) return DocumentRole.AUDIO;
    if (this.mimeType.includes("word") || this.mimeType.includes("document")) return DocumentRole.DOC;
    return DocumentRole.UNKNOWN;
  }

  isEditable(): boolean {
    return [GOOGLE_DOC_MIME, GOOGLE_SHEET_MIME, GOOGLE_SLIDE_MIME].includes(this.mimeType);
  }

  buildFromShared(data: IGoogleDriveSharedFileResponse): GoogleDriveDocument {
    this.build({
      id: data.id,
      name: data.name,
      mimeType: data.mimeType,
      size: data.size,
      modifiedTime: data.modifiedTime,
    });
    this.sharedWithMeTime = data.sharedWithMeTime;
    this.permissionRole = data.role;
    this.sharedOwners = data.owners || [];
    const owner: IGoogleDriveSharedOwner = this.sharedOwners[0];
    this.ownerDisplayName = owner ? owner.displayName : this.ownerDisplayName;
    return this;
  }

  initParent(): GoogleDriveDocument {
    const parent = new GoogleDriveDocument();
    parent.id = null;
    parent.name = lang.translate("google-drive.documents");
    parent.mimeType = GOOGLE_FOLDER_MIME;
    parent.isFolder = true;
    parent.type = DocumentsType.FOLDER;
    parent.role = DocumentRole.FOLDER;
    parent.ownerDisplayName = model.me.login;
    parent.modifiedTime = new Date().toISOString();
    parent.children = [];
    parent.cacheChildren = new models.CacheList<any>(0, () => false, () => false);
    parent.cacheChildren.setData([]);
    parent.cacheChildren.disableCache();
    parent.cacheDocument = new models.CacheList<any>(0, () => false, () => false);
    parent.cacheDocument.setData([]);
    parent.cacheDocument.disableCache();
    parent.isGoogleDriveParent = true;
    return parent;
  }

  static createStaticFolder(type: "trashbin" | "shared"): GoogleDriveDocument {
    const folder = new GoogleDriveDocument();
    folder.id = `__static__/${type}`;
    folder.name = lang.translate(`google-drive.static.${type}`);
    folder.mimeType = GOOGLE_FOLDER_MIME;
    folder.isFolder = true;
    folder.type = DocumentsType.FOLDER;
    folder.role = DocumentRole.FOLDER;
    folder.ownerDisplayName = model.me.login;
    folder.modifiedTime = new Date().toISOString();
    folder.children = [];
    folder.isStaticFolder = true;
    folder.staticFolderType = type;
    folder.cacheChildren = new models.CacheList<any>(0, () => false, () => false);
    folder.cacheChildren.setData([]);
    folder.cacheChildren.disableCache();
    folder.cacheDocument = new models.CacheList<any>(0, () => false, () => false);
    folder.cacheDocument.setData([]);
    folder.cacheDocument.disableCache();
    return folder;
  }

  static createRootGroup(): GoogleDriveDocument {
    const group = new GoogleDriveDocument();
    group.id = "__group__/root";
    group.name = lang.translate("google-drive.documents");
    group.mimeType = GOOGLE_FOLDER_MIME;
    group.isFolder = true;
    group.type = DocumentsType.FOLDER;
    group.role = DocumentRole.FOLDER;
    group.ownerDisplayName = model.me.login;
    group.modifiedTime = new Date().toISOString();
    group.children = [];
    group.isRootGroup = true;
    group.cacheChildren = new models.CacheList<any>(0, () => false, () => false);
    group.cacheChildren.setData([]);
    group.cacheChildren.disableCache();
    group.cacheDocument = new models.CacheList<any>(0, () => false, () => false);
    group.cacheDocument.setData([]);
    group.cacheDocument.disableCache();
    return group;
  }
}
