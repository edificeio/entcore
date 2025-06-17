import { AxiosError } from "axios";
import {
  angular,
  FolderPickerProps,
  FolderPickerSourceFile,
  model,
  toasts,
} from "entcore";
import { WorkspaceScope } from "../../../../controller";
import { models } from "../../../../services";
import { SyncDocument } from "../../models/nextcloudFolder.model";
import { INextcloudFolderScope } from "../../nextcloudFolder.directive";
import { nextcloudUserService } from "../../services/nextcloudUser.service";
import { nextcloudService } from "../../services/nextcloud.service";
import { safeApply } from "../../utils/safeApply.utils";
import { IWorkspaceNextcloudContent } from "./contentViewer.component";
import { ToolbarShareSnipletViewModel } from "./toolbarShare.components";

declare let window: any;

interface ILightbox {
  properties: boolean;
  delete: boolean;
  share: boolean;
  copy: boolean;
}

export interface IViewModel {
  lightbox: ILightbox;
  currentDocument: SyncDocument;

  // Document selection
  hasOneDocumentSelected(selectedDocuments: Array<SyncDocument>): boolean;
  isSelectedEditable(selectedDocuments: Array<SyncDocument>): boolean;

  // Actions
  downloadFiles(selectedDocuments: Array<SyncDocument>): void;
  openDocument(): void;
  viewFile: SyncDocument;

  editDocument(): void;

  // Properties/Rename
  toggleRenameView(
    state: boolean,
    selectedDocuments?: Array<SyncDocument>,
  ): void;
  renameDocument(): void;

  // Delete
  toggleDeleteView(state: boolean): void;
  deleteDocuments(): void;
  restoreDocuments(): void;

  // Copy/Move
  toggleCopyView(state: boolean, selectedDocuments?: Array<SyncDocument>): void;

  // Share
  share: any;
}

/**
 * ViewModel for toolbar actions in the Nextcloud content view
 */
export class ToolbarSnipletViewModel implements IViewModel {
  private vm: IWorkspaceNextcloudContent;
  private treeController: INextcloudFolderScope;
  private workspaceScope: WorkspaceScope;

  public lightbox: ILightbox;
  public currentDocument: SyncDocument;
  public viewFile: SyncDocument;

  public share: ToolbarShareSnipletViewModel;

  public copyProps: FolderPickerProps;

  constructor(scope: IWorkspaceNextcloudContent) {
    this.vm = scope;
    this.treeController = this.vm.getNextcloudTreeController();

    // Get workspace scope for accessing workspace trees
    this.workspaceScope = angular
      .element(document.querySelector('[data-ng-controller="Workspace"]'))
      .scope();

    // Initialize UI state
    this.lightbox = {
      properties: false,
      delete: false,
      share: false,
      copy: false,
    };
    this.currentDocument = null;

    this.share = new ToolbarShareSnipletViewModel(scope, this.lightbox);

    // Initialize empty copy props
    this.copyProps = {
      i18: null,
      sources: [],
      treeProvider: null,
      nextcloudTreeProvider: null,
      onCancel: () => this.closeCopyView(),
      onError: () => this.closeCopyView(),
    };
  }

  /*** Selection State ***/

  public isSelectedEditable(selectedDocuments: Array<SyncDocument>): boolean {
    return selectedDocuments.length > 0 && selectedDocuments[0].editable;
  }

  public hasOneDocumentSelected(
    selectedDocuments: Array<SyncDocument>,
  ): boolean {
    const total: number = selectedDocuments ? selectedDocuments.length : 0;
    return total === 1;
  }

  /*** Document Actions ***/

  public openDocument(): void {
    if (this.vm.selectedDocuments.length === 0) return;
    this.vm.openDocument();
  }

  public editDocument(): void {
    if (this.vm.selectedDocuments.length > 0) {
      nextcloudService.openNextcloudEditLink(
        this.vm.selectedDocuments[0],
        this.vm.nextcloudUrl,
      );
    }
  }

  public downloadFiles(selectedDocuments: Array<SyncDocument>): void {
    if (selectedDocuments.length === 1) {
      this.downloadSingleFile(selectedDocuments[0]);
    } else {
      this.downloadMultipleFiles(selectedDocuments);
    }
  }

  private downloadSingleFile(document: SyncDocument): void {
    window.open(
      nextcloudService.getFile(
        model.me.userId,
        document.name,
        document.path,
        document.contentType,
        document.isFolder,
      ),
    );
  }

  private downloadMultipleFiles(documents: Array<SyncDocument>): void {
    const selectedDocumentsName: Array<string> = documents.map(
      (doc: SyncDocument) => doc.name,
    );
    const getPathParent: string = this.vm.parentDocument.path || "/";

    window.open(
      nextcloudService.getFiles(
        model.me.userId,
        getPathParent,
        selectedDocumentsName,
      ),
    );
  }

  /*** Rename Operations ***/

  public toggleRenameView(
    state: boolean,
    selectedDocuments?: Array<SyncDocument>,
  ): void {
    this.lightbox.properties = state;
    if (state && selectedDocuments) {
      this.currentDocument = Object.assign({}, selectedDocuments[0]);
    } else {
      this.currentDocument = null;
    }
  }

  public renameDocument(): void {
    const oldDocumentToRename: SyncDocument = this.vm.selectedDocuments[0];
    if (!oldDocumentToRename) return;

    const parentPath = this.vm.parentDocument.path || "";
    const targetDocument: string =
      parentPath + "/" + encodeURIComponent(this.currentDocument.name);

    nextcloudService
      .moveDocument(model.me.userId, oldDocumentToRename.path, targetDocument)
      .then(() => this.refreshDocuments())
      .then(() => {
        this.toggleRenameView(false);
        this.vm.selectedDocuments = [];
        safeApply(this.vm);
      })
      .catch((err: AxiosError) => {
        this.handleError(
          err,
          "Error while attempting to rename document from content",
        );
        this.toggleRenameView(false);
        this.vm.selectedDocuments = [];
        safeApply(this.vm);
      });
  }

  /*** Delete Operations ***/

  public toggleDeleteView(state: boolean): void {
    this.lightbox.delete = state;
  }

  // This method moves documents to the trashbin
  public deleteDocuments(): void {
    const paths: Array<string> = this.vm.selectedDocuments.map(
      (selectedDocument: SyncDocument) => selectedDocument.path,
    );

    nextcloudService
      .deleteDocuments(model.me.userId, paths)
      .then(() => nextcloudUserService.getUserInfo(model.me.userId))
      .then((userInfos) => {
        this.treeController.userInfo = userInfos;
        toasts.info("nextcloud.documents.trash.confirmation");
        return this.refreshDocuments();
      })
      .then(() => {
        this.toggleDeleteView(false);
        this.vm.selectedDocuments = [];
        safeApply(this.vm);
      })
      .catch((err: AxiosError) => {
        this.handleError(
          err,
          "Error while attempting to delete documents from content",
        );
        this.toggleDeleteView(false);
        this.vm.selectedDocuments = [];
        safeApply(this.vm);
      });
  }

  public deleteDocumentsPermanently() {
    const paths: Array<string> = this.vm.selectedDocuments.map(
      (selectedDocument: SyncDocument) => selectedDocument.path,
    );

    nextcloudService
      .deleteTrashDocuments(model.me.userId, paths)
      .then(() => nextcloudUserService.getUserInfo(model.me.userId))
      .then((userInfos) => {
        this.treeController.userInfo = userInfos;
        toasts.info("nextcloud.documents.deletion.confirmation");
        return this.refreshTrashbin();
      })
      .then(() => {
        this.toggleDeleteView(false);
        this.vm.selectedDocuments = [];
        safeApply(this.vm);
      })
      .catch((err: AxiosError) => {
        this.handleError(
          err,
          "Error while attempting to delete documents from content",
        );
        this.toggleDeleteView(false);
        this.vm.selectedDocuments = [];
        safeApply(this.vm);
      });
  }

  public restoreDocuments(): void {
    const paths: Array<string> = this.vm.selectedDocuments.map(
      (selectedDocument: SyncDocument) => selectedDocument.path,
    );

    nextcloudService
      .restoreDocument(model.me.userId, paths)
      .then(() => nextcloudUserService.getUserInfo(model.me.userId))
      .then((userInfos) => {
        this.treeController.userInfo = userInfos;
        toasts.info("nextcloud.documents.restore.confirmation");
        return this.refreshTrashbin();
      })
      .then(() => {
        this.vm.selectedDocuments = [];
        safeApply(this.vm);
      })
      .catch((err: AxiosError) => {
        this.handleError(
          err,
          "Error while attempting to restore documents from content",
        );
        this.vm.selectedDocuments = [];
        safeApply(this.vm);
      });
  }

  /*** Copy Operations ***/
  public toggleCopyView(
    state: boolean,
    selectedDocuments?: Array<SyncDocument>,
  ): void {
    if (state && selectedDocuments) {
      this.setupCopyProps(selectedDocuments, "copy");
    }
    this.lightbox.copy = state;
  }

  private setupCopyProps(
    selectedDocuments: Array<SyncDocument>,
    type: "move" | "copy",
  ): void {
    this.copyProps = {
      i18: {
        title:
          type === "copy"
            ? "workspace.copy.window.title"
            : "workspace.move.window.title",
        actionTitle:
          type === "copy"
            ? "workspace.copy.window.action"
            : "workspace.move.window.action",
        actionProcessing:
          type === "copy" ? "workspace.copying" : "workspace.moving",
        actionFinished:
          type === "copy"
            ? "workspace.copy.finished"
            : "workspace.move.finished",
        info:
          type === "copy"
            ? "workspace.copy.window.info"
            : "workspace.move.window.info",
      },
      sources: selectedDocuments.map(
        (document: SyncDocument) =>
          ({
            action: type === "copy" ? "copy-from-file" : "move-from-file",
            fileId: document.path,
          }) as FolderPickerSourceFile,
      ),
      treeProvider: async () => {
        if (this.workspaceScope && this.workspaceScope.trees) {
          // Make sure we're returning an array of trees
          const trees = this.workspaceScope.trees.filter(
            (tree) => tree.filter === "owner",
          );
          return trees;
        }
        return [];
      },

      // If copying, we don't need to load Nextcloud folders
      nextcloudTreeProvider:
        type === "copy"
          ? null
          : async () => {
              try {
                const rootFolder = new SyncDocument().initParent();
                const documents = await nextcloudService.listDocument(
                  model.me.userId,
                );
                rootFolder.children = documents.filter(
                  (doc) => doc.isFolder && doc.path !== "/",
                );
                return [rootFolder];
              } catch (e) {
                console.error("Error loading Nextcloud folders", e);
                return [];
              }
            },

      submit: (selectedFolder: models.Element | SyncDocument) => {
        if (selectedFolder instanceof models.Element) {
          this.handleSubmitToWorkspace(selectedFolder, selectedDocuments, type);
        } else if (selectedFolder instanceof SyncDocument) {
          this.handleSubmitToNextcloud(selectedFolder, selectedDocuments, type);
        }
      },
      onCancel: () => this.closeCopyView(),
      onError: () => this.closeCopyView(),
    };
  }

  private async handleSubmitToWorkspace(
    destFolder: models.Element,
    sourceDocuments: Array<SyncDocument>,
    type: "copy" | "move",
  ): Promise<void> {
    try {
      const paths = sourceDocuments.map((doc) => doc.path);
      const parentId = destFolder._id;

      let results;
      if (type === "copy") {
        // Copy from Nextcloud to workspace
        results = await nextcloudService.copyDocumentToWorkspace(
          model.me.userId,
          paths,
          parentId,
        );
      } else {
        // Move from Nextcloud to workspace
        results = await nextcloudService.moveDocumentNextcloudToWorkspace(
          model.me.userId,
          paths,
          parentId,
        );
      }

      if (results && results.length > 0) {
        // Refresh the workspace folder if we're viewing it
        if (
          this.workspaceScope &&
          this.workspaceScope.openedFolder &&
          this.workspaceScope.openedFolder.folder &&
          this.workspaceScope.openedFolder.folder._id === parentId
        ) {
          this.workspaceScope.reloadFolderContent();
        }
      }

      // Clear selection and refresh Nextcloud view
      this.vm.selectedDocuments = [];
      await this.refreshDocuments();

      this.closeCopyView();
      this.vm.safeApply();
    } catch (err) {
      this.handleError(
        err,
        `Error ${type === "copy" ? "copying" : "moving"} to workspace`,
      );
      toasts.warning(`workspace.${type}.error`);
      this.closeCopyView();
      this.vm.safeApply();
    }
  }

  /**
   * Handle submission when a Nextcloud folder is selected
   */
  private async handleSubmitToNextcloud(
    destFolder: SyncDocument,
    sourceDocuments: Array<SyncDocument>,
    type: "move" | "copy",
  ): Promise<void> {
    try {
      const destPath = destFolder.path || "/";

      if (type === "move") {
        for (const doc of sourceDocuments) {
          await nextcloudService.moveDocument(
            model.me.userId,
            doc.path,
            `${destPath}/${doc.name}`,
          );
        }
        // Refresh views
        await this.refreshDocuments();
      } else {
        // Implement Nextcloud copy functionality here
      }

      this.closeCopyView();
      this.vm.safeApply();
    } catch (err) {
      this.handleError(
        err,
        `Error ${type === "copy" ? "copying" : "moving"} within Nextcloud`,
      );
      toasts.warning(`nextcloud.${type}.error`);
      this.closeCopyView();
      this.vm.safeApply();
    }
  }

  public closeCopyView(): void {
    this.lightbox.copy = false;

    if (this.copyProps && this.copyProps.sources) {
      this.copyProps.sources = [];
    }

    if (this.vm && this.vm.safeApply) {
      this.vm.safeApply();
    }
  }

  /*** Move Operations ***/
  public toggleMoveView(
    state: boolean,
    selectedDocuments?: Array<SyncDocument>,
  ): void {
    if (state && selectedDocuments) {
      this.setupCopyProps(selectedDocuments, "move");
    }
    this.lightbox.copy = state;
  }

  private async moveSubmit(selectedFolder: SyncDocument): Promise<void> {
    this.vm.selectedDocuments.forEach((document) => {
      nextcloudService.moveDocument(
        model.me.userId,
        document.path,
        selectedFolder.path + "/" + document.name,
      );
    });
    this.vm.selectedDocuments = [];
    await this.refreshDocuments();
    this.closeCopyView();
    this.vm.safeApply();
  }

  /*** Utility Methods ***/

  private async refreshDocuments(): Promise<Array<SyncDocument>> {
    const parentPath = this.vm.parentDocument.path;
    const syncDocuments = await nextcloudService.listDocument(
      model.me.userId,
      parentPath || null,
    );
    this.vm.documents = syncDocuments
      .filter((doc) => doc.path !== this.vm.parentDocument.path)
      .filter((doc_1) => doc_1.name !== model.me.userId);
    return syncDocuments;
  }

  private async refreshTrashbin(): Promise<void> {
    const syncDocuments = await nextcloudService.listTrash(model.me.userId);
    this.vm.documents = syncDocuments;
  }

  private handleError(err: AxiosError, message: string): void {
    console.error(
      `${message}: ${err.message}${this.getErrorMessage(err) ? `: ${this.getErrorMessage(err)}` : ""}`,
    );
    this.closeCopyView();
  }

  private getErrorMessage(err: AxiosError): string {
    return err?.response?.data?.message || "";
  }
}
