import { DocumentRole } from "../enums/documentRole.enum";
import { GoogleDriveDocument } from "../models/googleDriveDocument.model";
import { googleDriveService } from "../services/googleDrive.service";

export class GoogleDriveDocumentsUtils {
  // Recursively checks every descendant of a folder you own for one belonging to someone else —
  // trashing the folder would orphan it (Drive hides the folder from everyone but the collaborator's
  // own file isn't trashed itself, so they lose their only path to it). Depth-first with early exit as
  // soon as one is found, to avoid walking the whole tree when the answer is already known.
  static async hasForeignOwnedDescendant(userId: string, folderId: string): Promise<boolean> {
    const children = await googleDriveService.listDocument(userId, folderId);
    for (const child of children) {
      if (!child.ownedByMe) return true;
      if (child.isFolder && (await GoogleDriveDocumentsUtils.hasForeignOwnedDescendant(userId, child.id))) {
        return true;
      }
    }
    return false;
  }

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
