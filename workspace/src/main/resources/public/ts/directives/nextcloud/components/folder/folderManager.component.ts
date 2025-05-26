import { AxiosError } from "axios";
import { model, workspace } from "entcore";
import { SyncDocument } from "../../models/nextcloudFolder.model";
import { nextcloudService } from "../../services/nextcloud.service";
import { nextcloudEventService } from "../../services/nextcloudEvent.service";
import { safeApply } from "../../utils/safeApply.utils";
import models = workspace.v2.models;

interface ILightboxViewModel {
  folder: boolean;
}

interface IViewModel {
  lightbox: ILightboxViewModel;
  currentDocument: SyncDocument;
  toggleCreateFolder(state: boolean, folderCreate: models.Element): void;
  createFolder(folderCreate: models.Element): void;
}

export class FolderCreationModel implements IViewModel {
  private vm: any;
  private scope: any;

  lightbox: ILightboxViewModel;
  currentDocument: SyncDocument;

  constructor(scope) {
    this.vm = scope;
    this.lightbox = {
      folder: false,
    };
    this.currentDocument = null;
  }

  public toggleCreateFolder(
    state: boolean,
    folderCreate: models.Element,
  ): void {
    if (folderCreate) {
      folderCreate.name = "";
    }
    this.lightbox.folder = state;
  }

  public createFolder(folderCreate: models.Element): void {
    const folder: SyncDocument = this.vm.selectedFolder;
    nextcloudService
      .createFolder(
        model.me.userId,
        (folder.path != null ? folder.path + "/" : "") +
          encodeURI(folderCreate.name),
      )
      .then(() => {
        folderCreate.name = "";
        this.toggleCreateFolder(false, folderCreate);
        nextcloudEventService.sendOpenFolderDocument(this.vm.selectedFolder);
        safeApply(this.scope);
      })
      .catch((err: AxiosError) => {
        const message: string = "Error while attempting folder creation.";
        console.error(message + err.message);
      });
  }
}
