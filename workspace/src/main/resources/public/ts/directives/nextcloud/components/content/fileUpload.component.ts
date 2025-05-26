import { AxiosError } from "axios";
import { idiom as lang, model, notify } from "entcore";
import { SyncDocument } from "../../models/nextcloudFolder.model";
import { nextcloudUserService } from "../../services/nextcloudUser.service";
import { nextcloudService } from "../../services/nextcloud.service";
import { safeApply } from "../../utils/safeApply.utils";
import { IWorkspaceNextcloudContent } from "./contentViewer.component";

declare let window: any;

interface ILightboxViewModel {
  uploadFile: boolean;
  importFiles: boolean;
}

interface IViewModel {
  lightbox: ILightboxViewModel;
  uploadedDocuments: Array<File>;
  files: FileList;

  toggleUploadFilesView(state: boolean): void;
  toggleImportFilesView(state: boolean): void;
  startImportFlow(): void;
  onFilesSelectedFromImport(): void;
  onImportFiles(files: FileList): void;
  onValidImportFiles(files: FileList): void;

  // document util
  getSize(size: number): string;
  abortFile(doc: File): void;
}

export class UploadFileSnipletViewModel implements IViewModel {
  private vm: any;

  lightbox: ILightboxViewModel;
  uploadedDocuments: Array<File>;
  files: null;

  constructor(scope: IWorkspaceNextcloudContent) {
    this.vm = scope;
    this.lightbox = {
      importFiles: false,
      uploadFile: false,
    };
    this.uploadedDocuments = [];
  }

  toggleImportFilesView(state: boolean): void {
    this.lightbox.importFiles = state;
    this.vm.safeApply();
  }

  startImportFlow(): void {
    this.toggleImportFilesView(true);
  }

  onFilesSelectedFromImport(): void {
    this.toggleImportFilesView(false);
    this.onImportFiles(this.files);
  }

  toggleUploadFilesView(state: boolean): void {
    this.lightbox.uploadFile = state;
    if (!state) {
      this.uploadedDocuments = [];
      this.lightbox.importFiles = false;
    }
  }

  onImportFiles(files: FileList): void {
    this.toggleUploadFilesView(true);

    for (let i = 0; i < files.length; i++) {
      this.uploadedDocuments.push(files[i]);
    }
  }

  onValidImportFiles(): void {
    let selectedFolderFromNextcloudTree: SyncDocument =
      this.vm.getNextcloudTreeController()["selectedFolder"];
    const nextcloudController: any = this.vm.getNextcloudTreeController();
    nextcloudService
      .uploadDocuments(
        model.me.userId,
        this.uploadedDocuments,
        selectedFolderFromNextcloudTree.path,
      )
      .then(() => nextcloudUserService.getUserInfo(model.me.userId))
      .then((userInfos) => {
        nextcloudController.userInfo = userInfos;
        return nextcloudService.listDocument(
          model.me.userId,
          this.vm.parentDocument.path ? this.vm.parentDocument.path : null,
        );
      })
      .then((syncDocuments: Array<SyncDocument>) => {
        this.vm.documents = syncDocuments
          .filter(
            (syncDocument: SyncDocument) =>
              syncDocument.path != this.vm.parentDocument.path,
          )
          .filter(
            (syncDocument: SyncDocument) =>
              syncDocument.name != model.me.userId,
          );
        safeApply(this.vm);
      })
      .catch((err: AxiosError) => {
        const message: string = "Error while uploading files to nextcloud: ";
        console.error(
          `${message}${err.message}: ${this.vm.toolbar.getErrorMessage(err)}`,
        );
        if (err.message.includes("413") || err.message.includes("507")) {
          notify.error(lang.translate("file.too.large.upload"));
        } else {
          notify.error(lang.translate("nextcloud.fail.upload"));
        }
      });
    this.toggleUploadFilesView(false);
    safeApply(this.vm);
  }

  getSize(size: number): string {
    const koSize = size / 1024;
    if (koSize > 1024) {
      return parseInt(String((koSize / 1024) * 10)) / 10 + " Mo";
    }
    return Math.ceil(koSize) + " Ko";
  }

  abortFile(doc: File): void {
    const index: number = this.uploadedDocuments.indexOf(doc);
    this.uploadedDocuments.splice(index, 1);

    if (this.uploadedDocuments.length === 0) {
      this.toggleUploadFilesView(false);
    }
  }
}
