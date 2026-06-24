import { angular, Document, model, workspace } from "entcore";
import { WorkspaceEvent } from "entcore/types/src/ts/workspace/services";
import { models } from "../../../services";
import { GoogleDriveDocument } from "../models/googleDriveDocument.model";
import { GoogleDriveDocumentsUtils } from "./googleDriveDocuments.utils";
import ng = require("angular");

export class WorkspaceEntcoreUtils {
  static $ENTCORE_WORKSPACE: string = `div[data-ng-include="'folder-content'"]`;

  static toggleWorkspaceContentDisplay(state: boolean): void {
    const searchImportViewQuery: string =
      "section .margin-four > h3, section .margin-four > nav > div.row";
    Array.from(document.querySelectorAll(searchImportViewQuery)).forEach(
      (elem: Element) =>
        ((<HTMLElement>elem).style.display = state ? "block" : "none"),
    );

    const contentEmptyScreenQuery: string =
      "div .toggle-buttons-spacer .emptyscreen";
    Array.from(document.querySelectorAll(contentEmptyScreenQuery)).forEach(
      (elem: Element) =>
        ((<HTMLElement>elem).style.display = state ? "flex" : "none"),
    );

    const rightMagnetQuery: string = "app-title.twelve div.right-magnet";
    Array.from(document.querySelectorAll(rightMagnetQuery)).forEach(
      (elem: Element) =>
        ((<HTMLElement>elem).style.display = state ? "block" : "none"),
    );
  }

  static workspaceScope(): ng.IScope {
    return angular
      .element(document.getElementsByClassName("workspace-app"))
      .scope();
  }

  static updateWorkspaceDocuments(folder: any | models.Element): void {
    if (folder && folder instanceof models.Element) {
      if ("tree" in folder) {
        folder.eType = "folder";
      }
      const event: WorkspaceEvent = {
        action: "tree-change",
        elements: [folder],
      };
      workspace.v2.service.onChange.next(event);
    }
  }

  static toDocuments(syncDocuments: Array<GoogleDriveDocument>): Array<Document> {
    return syncDocuments.map((doc: GoogleDriveDocument) => {
      const elementObj: any = {
        name: doc.name,
        comments: "",
        metadata: {
          "content-type": doc.mimeType,
          role: doc.role,
          extension: GoogleDriveDocumentsUtils.getExtension(doc.name),
          filename: doc.name,
          size: doc.size,
        },
        owner: model.me.userId,
        ownerName: doc.ownerDisplayName,
      };
      const newElement: Document = new Document(elementObj);
      newElement.application = "google-drive";
      return newElement;
    });
  }
}
