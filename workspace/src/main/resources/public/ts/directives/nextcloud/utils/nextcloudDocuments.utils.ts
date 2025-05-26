import { model } from "entcore";
import { DocumentRole } from "../enums/documentRole.enum";
import { SyncDocument } from "../models/nextcloudFolder.model";

export class NextcloudDocumentsUtils {
  static determineRole(contentType: string): DocumentRole {
    for (let role in DocumentRole) {
      if (contentType.includes(DocumentRole[role])) {
        return <DocumentRole>DocumentRole[role];
      }
    }
    return DocumentRole.UNKNOWN;
  }

  static filterRemoveNameFile(): (syncDocument: SyncDocument) => boolean {
    return (syncDocument: SyncDocument) =>
      syncDocument.name !== model.me.userId;
  }

  static filterDocumentOnly(): (syncDocument: SyncDocument) => boolean {
    return (syncDocument: SyncDocument) =>
      syncDocument.isFolder && syncDocument.name != model.me.userId;
  }

  static filterFilesOnly(): (syncDocument: SyncDocument) => boolean {
    return (syncDocument: SyncDocument) =>
      !syncDocument.isFolder && syncDocument.name != model.me.userId;
  }

  static filterRemoveOwnDocument(
    document: SyncDocument,
  ): (syncDocument: SyncDocument) => boolean {
    return (syncDocument: SyncDocument) => syncDocument.path !== document.path;
  }

  static getExtension(filename: string): string {
    let words: Array<string> = filename.split(".");
    return words[words.length - 1];
  }
}
