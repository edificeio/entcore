import { angular, Document, model, workspace } from "entcore";
import { WorkspaceEvent } from "entcore/types/src/ts/workspace/services";
import { models } from "../../../services";
import { SyncDocument } from "../models/nextcloudFolder.model";
import { NextcloudDocumentsUtils } from "./nextcloudDocuments.utils";
import ng = require("angular");

export class WorkspaceEntcoreUtils {
  static $ENTCORE_WORKSPACE: string = `div[data-ng-include="'folder-content'"]`;

  /**
   * Will fetch <progress-bar> Element type component and its div to toggle hide or show depending on state
   * Format date based on given format using moment
   * @param state boolean determine display default or none
   */
  static toggleProgressBarDisplay(state: boolean): void {
    const htmlQuery: string = ".mobile-navigation > div.row";
    (<HTMLElement>document.querySelector(htmlQuery)).style.display = state
      ? "block"
      : "none";
  }

  /**
   * Will fetch all buttons in workspace folder its div to toggle hide or show depending on state
   * @param state boolean determine display default or none
   */
  static toggleWorkspaceButtonsDisplay(state: boolean): void {
    const htmlQuery: string = `.mobile-navigation > a, .zero-mobile > div, sniplet[application="lool"`;
    Array.from(document.querySelectorAll(htmlQuery)).forEach(
      (elem: Element) =>
        ((<HTMLElement>elem).style.display = state ? "block" : "none"),
    );
  }

  /**
   * Will fetch all buttons in workspace folder its div to toggle hide or show depending on state
   * @param state boolean determine display default or none
   */
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

  /**
   * Fetch workspace controller scope
   */
  static workspaceScope(): ng.IScope {
    return angular
      .element(document.getElementsByClassName("workspace-app"))
      .scope();
  }

  /**
   * Update fetch folder content via workspace controller
   * @param folder folder from workspace controller
   */
  static updateWorkspaceDocuments(folder: any | models.Element): void {
    if (folder && folder instanceof models.Element) {
      //The root folder is treated differently because it contains all the tree of files and folders, and we can't apply
      // the same treatment as if it was a classic folder.
      //As it is not a folder, it does not contain the eType attribute, so we have to simulate it by adding the eType attribute,
      // and make the refresh happen on this root folder.
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

  static toDocuments(syncDocuments: Array<SyncDocument>): Array<Document> {
    let formattedDocuments: Array<Document> = [];
    syncDocuments.forEach((syncDoc: SyncDocument) => {
      let elementObj: any = {
        name: syncDoc.name,
        comments: "",
        metadata: {
          "content-type": syncDoc.contentType,
          role: syncDoc.role,
          extension: NextcloudDocumentsUtils.getExtension(syncDoc.name),
          filename: syncDoc.name,
          size: syncDoc.size,
        },
        owner: model.me.userId,
        ownerName: syncDoc.ownerDisplayName,
        path: syncDoc.path,
      };
      let newElement: Document = new Document(elementObj);
      newElement.application = "nextcloud";
      formattedDocuments.push(newElement);
    });
    return formattedDocuments;
  }
}
