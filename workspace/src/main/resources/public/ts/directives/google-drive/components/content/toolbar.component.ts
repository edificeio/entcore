import { AxiosError } from "axios";
import {
  angular,
  FolderPickerProps,
  FolderPickerSourceFile,
  idiom as lang,
  model,
  toasts,
} from "entcore";
import { WorkspaceScope } from "../../../../controller";
import { models } from "../../../../services";
import { GoogleDriveDocument } from "../../models/googleDriveDocument.model";
import { googleDriveEventService } from "../../services/googleDriveEvent.service";
import { googleDriveService } from "../../services/googleDrive.service";
import { safeApply } from "../../utils/safeApply.utils";
import { ToolbarShareGoogleDriveViewModel } from "./toolbarShare.component";

declare let window: any;

interface ILightbox {
  delete: boolean;
  copy: boolean;
  share: boolean;
  rename: boolean;
}

export interface IToolbarViewModel {
  lightbox: ILightbox;
  currentDocument: GoogleDriveDocument;

  hasOneDocumentSelected(selectedDocuments: Array<GoogleDriveDocument>): boolean;
  isSelectedEditable(selectedDocuments: Array<GoogleDriveDocument>): boolean;
  isSelectedRemovableFromSharedList(selectedDocuments: Array<GoogleDriveDocument>): boolean;
  isSelectedDuplicatable(selectedDocuments: Array<GoogleDriveDocument>): boolean;
  isSelectedOwnedByMe(selectedDocuments: Array<GoogleDriveDocument>): boolean;
  isSelectedExportable(selectedDocuments: Array<GoogleDriveDocument>): boolean;

  downloadFiles(selectedDocuments: Array<GoogleDriveDocument>): void;
  openDocument(): void;
  editDocument(): void;

  toggleRenameView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void;
  renameDocument(): void;

  toggleDeleteView(state: boolean): void;
  deleteDocuments(): void;
  deleteDocumentsPermanently(): void;
  restoreDocuments(): void;

  toggleCopyView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void;
  toggleMoveView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void;
  toggleDuplicateView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void;
}

export class ToolbarSnipletViewModel implements IToolbarViewModel {
  private vm: any;
  public treeController: any;
  private workspaceScope: WorkspaceScope;

  public lightbox: ILightbox;
  public currentDocument: GoogleDriveDocument;
  public copyProps: FolderPickerProps;
  public share: ToolbarShareGoogleDriveViewModel;

  constructor(scope: any) {
    this.vm = scope;
    this.treeController = this.vm.getGoogleDriveTreeController();

    this.workspaceScope = angular
      .element(document.querySelector('[data-ng-controller="Workspace"]'))
      .scope();

    this.lightbox = {
      delete: false,
      copy: false,
      share: false,
      rename: false,
    };
    this.currentDocument = null;

    this.share = new ToolbarShareGoogleDriveViewModel(scope, this.lightbox);

    this.copyProps = {
      i18: null,
      sources: [],
      treeProvider: null,
      nextcloudTreeProvider: null,
      onCancel: () => this.closeCopyView(),
      onError: () => this.closeCopyView(),
    };
  }

  public isSelectedEditable(selectedDocuments: Array<GoogleDriveDocument>): boolean {
    return (
      selectedDocuments.length > 0 &&
      selectedDocuments[0].editable &&
      selectedDocuments[0].permissionRole !== "reader"
    );
  }

  // Matches the per-tile "..." menu's own gating — removing a read-only shared item isn't supported
  // by the Drive API under this app's auth model (see removeFromSharedList's backend comment).
  // A Google Form/Vids/etc. has no export format Drive can convert it to — see
  // GoogleDriveDocument.isExportableToWorkspace.
  public isSelectedExportable(selectedDocuments: Array<GoogleDriveDocument>): boolean {
    return (
      selectedDocuments.length > 0 &&
      selectedDocuments.every((doc) => doc.isExportableToWorkspace())
    );
  }

  // A file inside your own folder isn't necessarily yours (e.g. created there by someone you shared
  // the folder with as editor) — only the actual owner can trash it via the Drive API.
  public isSelectedOwnedByMe(selectedDocuments: Array<GoogleDriveDocument>): boolean {
    return (
      selectedDocuments.length > 0 &&
      selectedDocuments.every((doc) => doc.ownedByMe)
    );
  }

  // Drive's native files().copy() can't duplicate a folder (no recursive copy in a single call).
  public isSelectedDuplicatable(selectedDocuments: Array<GoogleDriveDocument>): boolean {
    return (
      selectedDocuments.length > 0 &&
      selectedDocuments.every((doc) => !doc.isFolder)
    );
  }

  public isSelectedRemovableFromSharedList(selectedDocuments: Array<GoogleDriveDocument>): boolean {
    return (
      selectedDocuments.length > 0 &&
      selectedDocuments.every((doc) => doc.permissionRole !== "reader" && doc.isDirectlyShared)
    );
  }

  public hasOneDocumentSelected(selectedDocuments: Array<GoogleDriveDocument>): boolean {
    return selectedDocuments ? selectedDocuments.length === 1 : false;
  }

  public openDocument(): void {
    if (this.vm.selectedDocuments.length === 0) return;
    // Branches to folder navigation vs file preview — this.vm.openDocument() always opened the
    // preview, even for a folder (e.g. a folder shared with the user, from "Partagé avec moi").
    this.vm.onOpenContent(this.vm.selectedDocuments[0]);
  }

  public editDocument(): void {
    if (this.vm.selectedDocuments.length > 0) {
      googleDriveService.openEditLink(model.me.userId, this.vm.selectedDocuments[0]);
    }
  }

  public downloadFiles(selectedDocuments: Array<GoogleDriveDocument>): void {
    if (selectedDocuments.length === 1) {
      this.downloadSingleFile(selectedDocuments[0]);
    } else {
      this.downloadMultipleFiles(selectedDocuments);
    }
  }

  private downloadSingleFile(document: GoogleDriveDocument): void {
    window.open(
      googleDriveService.getFile(model.me.userId, document.id, document.isFolder),
    );
  }

  private downloadMultipleFiles(documents: Array<GoogleDriveDocument>): void {
    const ids = documents.map((doc) => doc.id);
    window.open(googleDriveService.getFiles(model.me.userId, ids));
  }

  public toggleRenameView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void {
    this.lightbox.rename = state;
    if (state && selectedDocuments) {
      this.currentDocument = Object.assign({}, selectedDocuments[0]);
    } else {
      this.currentDocument = null;
    }
  }

  public renameDocument(): void {
    const documentToRename: GoogleDriveDocument = this.vm.selectedDocuments[0];
    if (!documentToRename || !this.currentDocument) return;

    googleDriveService
      .renameDocument(model.me.userId, documentToRename.id, this.currentDocument.name)
      .then(() => this.refreshDocuments())
      .then(() => {
        this.toggleRenameView(false);
        this.vm.selectedDocuments = [];
        googleDriveEventService.sendOpenFolderDocument(this.vm.parentDocument);
        safeApply(this.vm);
      })
      .catch((err: AxiosError) => {
        console.error("Error while attempting to rename document: " + err.message);
        this.toggleRenameView(false);
        this.vm.selectedDocuments = [];
        safeApply(this.vm);
      });
  }

  public toggleDeleteView(state: boolean): void {
    this.lightbox.delete = state;
  }

  public deleteDocuments(): void {
    const ids = this.vm.selectedDocuments.map((doc: GoogleDriveDocument) => doc.id);
    this.vm.isDeleting = true;
    safeApply(this.vm);
    // In shared mode this trashes the owner's file (files().update trashed=true) and 500s when the
    // caller only has read/comment access — removing a shared item should only revoke our own access.
    const isSharedMode = this.vm.isSharedMode();
    const deleteCall = isSharedMode
      ? googleDriveService.removeFromSharedList(model.me.userId, ids)
      : googleDriveService.deleteDocuments(model.me.userId, ids);
    deleteCall
      .then(() => {
        toasts.info(
          isSharedMode
            ? "google-drive.documents.remove.shared.confirmation"
            : "google-drive.documents.trash.confirmation",
        );
        // refreshDocuments() assumes a real Drive folder id — in shared mode, parentDocument can be
        // the "__static__/shared" synthetic node, which isn't a real id and 500s the generic listing.
        return isSharedMode ? this.refreshSharedFiles() : this.refreshDocuments();
      })
      .then(() => {
        this.toggleDeleteView(false);
        this.vm.selectedDocuments = [];
        // Refreshes the sidebar tree too, mirroring createFolder's own refresh.
        googleDriveEventService.sendOpenFolderDocument(this.vm.parentDocument);
      })
      .catch((err: AxiosError) => {
        console.error("Error while attempting to delete documents: " + err.message);
        this.toggleDeleteView(false);
        this.vm.selectedDocuments = [];
      })
      .then(() => {
        this.vm.isDeleting = false;
        safeApply(this.vm);
      });
  }

  public deleteDocumentsPermanently(): void {
    const ids = this.vm.selectedDocuments.map((doc: GoogleDriveDocument) => doc.id);
    this.vm.isDeleting = true;
    safeApply(this.vm);
    googleDriveService
      .deleteTrashDocuments(model.me.userId, ids)
      .then(() => {
        toasts.info("google-drive.documents.deletion.confirmation");
        return this.refreshTrashbin();
      })
      .then(() => {
        this.toggleDeleteView(false);
        this.vm.selectedDocuments = [];
        googleDriveEventService.requestQuotaRefresh();
      })
      .catch((err: AxiosError) => {
        console.error("Error while attempting to permanently delete documents: " + err.message);
        this.toggleDeleteView(false);
        this.vm.selectedDocuments = [];
      })
      .then(() => {
        this.vm.isDeleting = false;
        safeApply(this.vm);
      });
  }

  public restoreDocuments(): void {
    const ids = this.vm.selectedDocuments.map((doc: GoogleDriveDocument) => doc.id);
    this.vm.isRestoring = true;
    safeApply(this.vm);
    googleDriveService
      .restoreDocument(model.me.userId, ids)
      .then(() => {
        toasts.info("google-drive.documents.restore.confirmation");
        return this.refreshTrashbin();
      })
      .then(() => {
        this.vm.selectedDocuments = [];
      })
      .catch((err: AxiosError) => {
        console.error("Error while attempting to restore documents: " + err.message);
        this.vm.selectedDocuments = [];
      })
      .then(() => {
        this.vm.isRestoring = false;
        safeApply(this.vm);
      });
  }

  public toggleCopyView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void {
    if (state && selectedDocuments) {
      this.setupCopyProps(selectedDocuments, "copy");
    }
    this.lightbox.copy = state;
  }

  public toggleMoveView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void {
    if (state && selectedDocuments) {
      this.setupCopyProps(selectedDocuments, "move");
    }
    this.lightbox.copy = state;
  }

  // Duplicates file(s) into a different folder, staying within Google Drive — unlike "copy" (export
  // to the personal workspace) and "move" (relocates, no duplicate, can also target the workspace).
  public toggleDuplicateView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void {
    if (state && selectedDocuments) {
      this.setupCopyProps(selectedDocuments, "duplicate");
    }
    this.lightbox.copy = state;
  }

  private setupCopyProps(
    selectedDocuments: Array<GoogleDriveDocument>,
    type: "move" | "copy" | "duplicate",
  ): void {
    const i18 = {
      move: {
        title: "workspace.move.window.title",
        actionTitle: "workspace.move.window.action",
        actionProcessing: "workspace.moving",
        actionFinished: "workspace.move.finished",
        info: "workspace.move.window.info",
      },
      copy: {
        title: "google-drive.export.window.title",
        actionTitle: "google-drive.export.window.action",
        actionProcessing: "google-drive.exporting",
        actionFinished: "google-drive.export.window.finished",
        info: "google-drive.export.window.info",
      },
      duplicate: {
        title: "google-drive.duplicate.window.title",
        actionTitle: "google-drive.duplicate.window.action",
        actionProcessing: "google-drive.duplicating",
        actionFinished: "google-drive.duplicate.window.finished",
        info: "google-drive.duplicate.window.info",
      },
    }[type];
    this.copyProps = {
      i18,
      sources: selectedDocuments.map(
        (doc: GoogleDriveDocument) =>
          ({
            action: type === "copy" || type === "duplicate" ? "copy-from-file" : "move-from-file",
            fileId: doc.id,
          }) as FolderPickerSourceFile,
      ),
      // "duplicate" must stay within Google Drive only — no personal-workspace destination offered.
      treeProvider: type === "duplicate"
        ? async () => []
        : async () => {
            const ownerTrees = this.workspaceScope && this.workspaceScope.trees
              ? this.workspaceScope.trees.filter((tree) => tree.filter === "owner")
              : [];
            // Mirrors classic space's own copy/move picker: wraps "Mes documents" under a labeled
            // "Espace personnel" root instead of showing it bare (and lets folderTree2.ts render the
            // home icon for it via isPersonalSpaceWrapper).
            const ownerWrapper = {
              name: lang.translate("workspace.personal.space"),
              children: ownerTrees,
              isPersonalSpaceWrapper: true,
            };
            return [ownerWrapper] as any;
          },
      nextcloudTreeProvider: type === "copy"
        ? null
        : async () => {
            try {
              const rootFolder = new GoogleDriveDocument().initParent();
              const documents = await googleDriveService.listDocument(model.me.userId);
              rootFolder.children = documents.filter((doc) => doc.isFolder);
              return [rootFolder];
            } catch (e) {
              console.error("Error loading Google Drive folders", e);
              return [];
            }
          },
      submit: (selectedFolder: models.Element | GoogleDriveDocument) => {
        if (selectedFolder instanceof models.Element) {
          return this.handleSubmitToWorkspace(selectedFolder, selectedDocuments, type as "copy" | "move");
        } else if (selectedFolder instanceof GoogleDriveDocument) {
          return this.handleSubmitToGoogleDrive(selectedFolder, selectedDocuments, type as "move" | "duplicate");
        }
      },
      onCancel: () => this.closeCopyView(),
      onError: () => this.closeCopyView(),
    };
  }

  private async handleSubmitToWorkspace(
    destFolder: models.Element,
    sourceDocuments: Array<GoogleDriveDocument>,
    type: "copy" | "move",
  ): Promise<void> {
    try {
      const ids = sourceDocuments.map((doc) => doc.id);
      const parentId = destFolder._id;

      let results: Array<models.Element>;
      if (type === "copy") {
        results = await googleDriveService.copyDocumentToWorkspace(model.me.userId, ids, parentId);
      } else {
        results = await googleDriveService.moveDocumentDriveToWorkspace(model.me.userId, ids, parentId);
      }

      if (results && results.length > 0 && this.workspaceScope?.openedFolder?.folder?._id === parentId) {
        this.workspaceScope.reloadFolderContent();
      }

      // The backend copies/moves each document independently and only reports the ones that
      // succeeded (e.g. a Google Form/Vids has no export format Drive can convert it to) — without
      // this, a partial failure looks identical to full success: no error, but the file never appears.
      if (results && results.length < sourceDocuments.length) {
        toasts.warning(`workspace.${type}.partial.error`);
      }

      this.vm.selectedDocuments = [];
      await this.refreshDocuments();
      this.closeCopyView();
      safeApply(this.vm);
    } catch (err) {
      const e = err as AxiosError;
      console.error(`Error ${type === "copy" ? "copying" : "moving"} to workspace: ` + e.message);
      toasts.warning(`workspace.${type}.error`);
      this.closeCopyView();
      safeApply(this.vm);
    }
  }

  // Reached for "move" and "duplicate" — a Google Drive destination is never offered when
  // exporting/copying to the workspace (setupCopyProps leaves the picker's Drive tree unset for "copy").
  private async handleSubmitToGoogleDrive(
    destFolder: GoogleDriveDocument,
    sourceDocuments: Array<GoogleDriveDocument>,
    type: "move" | "duplicate",
  ): Promise<void> {
    try {
      for (const doc of sourceDocuments) {
        if (type === "duplicate") {
          await googleDriveService.copyDocumentWithinDrive(model.me.userId, doc.id, destFolder.id);
        } else {
          await googleDriveService.moveDocument(model.me.userId, doc.id, destFolder.id);
        }
      }
      await this.refreshDocuments();
      this.closeCopyView();
      safeApply(this.vm);
    } catch (err) {
      const e = err as AxiosError;
      console.error(`Error ${type === "duplicate" ? "duplicating" : "moving"} within Google Drive: ` + e.message);
      toasts.warning(type === "duplicate" ? "google-drive.duplicate.error" : "google-drive.move.error");
      this.closeCopyView();
      safeApply(this.vm);
    }
  }

  public closeCopyView(): void {
    this.lightbox.copy = false;
    if (this.copyProps?.sources) {
      this.copyProps.sources = [];
    }
    safeApply(this.vm);
  }

  public getErrorMessage(err: AxiosError): string {
    return (err?.response?.data as any)?.message || "";
  }

  private async refreshDocuments(): Promise<Array<GoogleDriveDocument>> {
    const parentId = this.vm.parentDocument?.id || null;
    const docs = await googleDriveService.listDocument(model.me.userId, parentId);
    this.vm.documents = docs.filter(
      (doc) => doc.id !== this.vm.parentDocument?.id,
    );
    return docs;
  }

  private async refreshTrashbin(): Promise<void> {
    this.vm.documents = await googleDriveService.listTrash(model.me.userId);
  }

  private async refreshSharedFiles(): Promise<Array<GoogleDriveDocument>> {
    // Top-level "Partagé avec moi" (synthetic "__static__/shared" node): flat shared-files listing.
    // A real shared folder navigated into (own Drive id, see googleDriveFolder.directive.ts's
    // permissionRole propagation): the generic listing works fine via its real id.
    const docs = this.vm.parentDocument?.isStaticFolder
      ? await googleDriveService.listSharedFiles(model.me.userId)
      : await googleDriveService.listDocument(model.me.userId, this.vm.parentDocument?.id || null);
    this.vm.documents = docs;
    return docs;
  }
}
