import { idiom as lang, model, workspace } from "entcore";
import { DocumentRole } from "../enums/document-role";
import { DocumentsType } from "../enums/documents-type";
import { NextcloudDocumentsUtils } from "../utils/nextcloud-documents.utils";
import models = workspace.v2.models;

export interface IDocumentResponse {
  path: string;
  displayname: string;
  ownerDisplayName: string;
  contentType: string;
  size: number;
  favorite: number;
  etag: string;
  fileId: number;
  isFolder: boolean;
  lastModified: string;
}

export class SyncDocument {
  path: string;
  name: string;
  ownerDisplayName: string;
  contentType: string;
  role: DocumentRole;
  size: number;
  favorite: number;
  editable: boolean;
  etag: string;
  extension: string;
  fileId: number;
  isFolder: boolean;
  lastModified: string;
  type: DocumentsType;
  children: Array<SyncDocument>;
  cacheChildren: models.CacheList<any>;
  cacheDocument: models.CacheList<any>;

  // custom field bound by other entity/model
  selected?: boolean;
  isNextcloudParent?: boolean;

  build(data: IDocumentResponse): SyncDocument {
    this.name = decodeURIComponent(data.displayname);
    this.ownerDisplayName = data.ownerDisplayName;
    this.path = data.path.split(model.me.userId).pop();
    this.contentType = data.contentType;
    this.size = data.size;
    this.favorite = data.favorite;
    this.etag = data.etag;
    this.fileId = data.fileId;
    this.isFolder = data.isFolder;
    this.lastModified = data.lastModified;
    this.type = this.determineType();
    this.role = this.determineRole();
    this.editable = this.isEditable();
    this.children = [];
    this.cacheChildren = new models.CacheList<any>(
      0,
      () => false,
      () => false,
    );
    this.cacheChildren.setData([]);
    this.cacheChildren.disableCache();
    this.cacheDocument = new models.CacheList<any>(
      0,
      () => false,
      () => false,
    );
    this.cacheDocument.setData([]);
    this.cacheDocument.disableCache();

    return this;
  }

  determineRole(): DocumentRole {
    if (this.isFolder) {
      return DocumentRole.FOLDER;
    } else if (this.contentType) {
      return NextcloudDocumentsUtils.determineRole(this.contentType);
    } else {
      return DocumentRole.UNKNOWN;
    }
  }

  determineType(): DocumentsType {
    if (this.isFolder) {
      return DocumentsType.FOLDER;
    } else {
      return DocumentsType.FILE;
    }
  }

  isEditable(): boolean {
    return (<any>[
      DocumentRole.DOC,
      DocumentRole.PDF,
      DocumentRole.XLS,
      DocumentRole.PPT,
    ]).includes(this.role);
  }

  // create a folder with only one content (synchronized document) and its children all sync documents
  initParent(): SyncDocument {
    const parentNextcloudFolder: SyncDocument = new SyncDocument();
    parentNextcloudFolder.path = null;
    parentNextcloudFolder.name = lang.translate("nextcloud.documents");
    parentNextcloudFolder.ownerDisplayName = model.me.login;
    parentNextcloudFolder.contentType = null;
    parentNextcloudFolder.size = null;
    parentNextcloudFolder.favorite = null;
    parentNextcloudFolder.etag = null;
    parentNextcloudFolder.fileId = null;
    parentNextcloudFolder.isFolder = true;
    parentNextcloudFolder.lastModified = new Date().toISOString();
    parentNextcloudFolder.children = [];
    parentNextcloudFolder.cacheChildren = new models.CacheList<any>(
      0,
      () => false,
      () => false,
    );
    parentNextcloudFolder.cacheChildren.setData([]);
    parentNextcloudFolder.cacheChildren.disableCache();
    parentNextcloudFolder.cacheDocument = new models.CacheList<any>(
      0,
      () => false,
      () => false,
    );
    parentNextcloudFolder.cacheDocument.setData([]);
    parentNextcloudFolder.cacheDocument.disableCache();

    parentNextcloudFolder.isNextcloudParent = true;
    return parentNextcloudFolder;
  }
}
