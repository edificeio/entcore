import { DocumentRole } from "../enums/documentRole.enum";
import { GoogleDriveDocument } from "../models/googleDriveDocument.model";

export class GoogleDriveDocumentsUtils {
  static filterDocumentOnly(): (doc: GoogleDriveDocument) => boolean {
    return (doc: GoogleDriveDocument) => doc.isFolder;
  }

  static filterFilesOnly(): (doc: GoogleDriveDocument) => boolean {
    return (doc: GoogleDriveDocument) => !doc.isFolder;
  }

  static filterRemoveOwnDocument(
    document: GoogleDriveDocument,
  ): (doc: GoogleDriveDocument) => boolean {
    return (doc: GoogleDriveDocument) => doc.id !== document.id;
  }

  static getExtension(filename: string): string {
    const parts = filename.split(".");
    return parts[parts.length - 1];
  }
}
