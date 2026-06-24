import { AxiosError } from "axios";
import { model, workspace } from "entcore";
import { GoogleDriveDocument } from "../../models/googleDriveDocument.model";
import { googleDriveService } from "../../services/googleDrive.service";
import { googleDriveEventService } from "../../services/googleDriveEvent.service";
import { safeApply } from "../../utils/safeApply.utils";
import models = workspace.v2.models;

interface ILightboxViewModel {
  folder: boolean;
}

interface IViewModel {
  lightbox: ILightboxViewModel;
  currentDocument: GoogleDriveDocument;
  toggleCreateFolder(state: boolean, folderCreate: models.Element): void;
  createFolder(folderCreate: models.Element): void;
}

export class FolderCreationModel implements IViewModel {
  private vm: any;

  lightbox: ILightboxViewModel;
  currentDocument: GoogleDriveDocument;

  constructor(scope: any) {
    this.vm = scope;
    this.lightbox = { folder: false };
    this.currentDocument = null;
  }

  public toggleCreateFolder(state: boolean, folderCreate: models.Element): void {
    if (folderCreate) {
      folderCreate.name = "";
    }
    this.lightbox.folder = state;
  }

  public createFolder(folderCreate: models.Element): void {
    const selectedFolder: GoogleDriveDocument = this.vm.selectedFolder;
    const parentId = selectedFolder && !selectedFolder.isGoogleDriveParent
      ? selectedFolder.id
      : undefined;

    googleDriveService
      .createFolder(model.me.userId, folderCreate.name, parentId)
      .then(() => {
        folderCreate.name = "";
        this.toggleCreateFolder(false, folderCreate);
        googleDriveEventService.sendOpenFolderDocument(this.vm.selectedFolder);
        safeApply(this.vm);
      })
      .catch((err: AxiosError) => {
        console.error("Error while attempting folder creation: " + err.message);
      });
  }
}
