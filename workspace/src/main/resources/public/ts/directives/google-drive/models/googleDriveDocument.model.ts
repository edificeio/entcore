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
  ownedByMe?: boolean;
  ownerUserId?: string;
  ownerDisplayName?: string;
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

  // True only for a top-level "Shared with me" item (buildFromShared()) — it alone holds a direct
  // Drive permission entry for the current user. Children of a shared folder only inherit
  // permissionRole (propagated in googleDriveFolder.directive.ts) for display/gating purposes, not
  // this flag: Drive gives them no permission entry of their own, so "remove from shared list"
  // (which looks up and deletes that entry) can't work on them — see removeFromSharedList's backend.
  isDirectlyShared?: boolean;

  // A file inside your own folder isn't necessarily yours: whoever created it (e.g. an editor you
  // shared the folder with) remains its owner. Defaults true (own root/static nodes never go through
  // build(), and absence of the field — e.g. from buildFromShared's own owners check — shouldn't
  // wrongly hide actions), set from the real Drive owners list for documents fetched via listFiles.
  ownedByMe: boolean = true;
  ownerUserId?: string;

  build(data: IGoogleDriveDocumentResponse): GoogleDriveDocument {
    this.id = data.id;
    this.name = data.name;
    this.mimeType = data.mimeType || "";
    this.size = data.size;
    this.modifiedTime = data.modifiedTime;
    this.isShared = !!data.shared;
    if (data.ownedByMe !== undefined) this.ownedByMe = data.ownedByMe;
    this.ownerUserId = data.ownerUserId;
    this.isFolder = this.mimeType === GOOGLE_FOLDER_MIME;
    this.ownerDisplayName = data.ownerDisplayName || model.me.login;
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

  // Exporting to the personal workspace needs an actual downloadable file: a non-Google mimetype
  // downloads as-is, but a Google-native one must be converted first (files().export), and the Drive
  // API only supports that conversion for Docs/Sheets/Slides — not Forms, Vids, Drawings, Apps Script,
  // etc. Exporting one of those silently drops the file today (see DefaultDocumentsService.getFile's
  // resolveExportMimeType falling back to a PDF export that Drive itself then rejects).
  isExportableToWorkspace(): boolean {
    if (this.isFolder) return true;
    if (!this.mimeType.startsWith("application/vnd.google-apps.")) return true;
    return [GOOGLE_DOC_MIME, GOOGLE_SHEET_MIME, GOOGLE_SLIDE_MIME].includes(this.mimeType);
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
    this.isDirectlyShared = true;
    // build() defaults this true (an own, normally-listed document) — a shared-with-me item never is.
    this.ownedByMe = false;
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
