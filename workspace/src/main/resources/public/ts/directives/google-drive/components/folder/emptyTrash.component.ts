import { model } from "entcore";
import { googleDriveEventService } from "../../services/googleDriveEvent.service";
import { googleDriveService } from "../../services/googleDrive.service";
import { safeApply } from "../../utils/safeApply.utils";

interface ILightboxViewModel {
  emptyTrash: boolean;
}

interface IViewModel {
  lightbox: ILightboxViewModel;
}

export class EmptyTrashModel implements IViewModel {
  private vm: any;

  lightbox: ILightboxViewModel;

  constructor(scope: any) {
    this.vm = scope;
    this.lightbox = { emptyTrash: false };
  }

  public toggleDeleteView(visible: boolean): void {
    this.lightbox.emptyTrash = visible;
    safeApply(this.vm);
  }

  public emptyTrashbin(): void {
    googleDriveService
      .deleteTrash(model.me.userId)
      .then(() => {
        googleDriveEventService.sendDocuments({
          parentDocument: null,
          documents: [],
        });
        googleDriveEventService.requestQuotaRefresh();
        this.lightbox.emptyTrash = false;
        safeApply(this.vm);
      })
      .catch((err: Error) => {
        console.error("Error while attempting to empty trashbin: " + err.message);
      });
  }
}
