import { model } from "entcore";
import { INextcloudFolderScope } from "../../nextcloudFolder.directive";
import { nextcloudEventService } from "../../services/nextcloudEvent.service";
import { nextcloudService } from "../../services/nextcloud.service";
import { safeApply } from "../../utils/safeApply.utils";

interface ILightboxViewModel {
  emptyTrash: boolean;
}

interface IViewModel {
  lightbox: ILightboxViewModel;
}

export class EmptyTrashModel implements IViewModel {
  private vm: INextcloudFolderScope;

  lightbox: ILightboxViewModel;

  constructor(scope: INextcloudFolderScope) {
    this.vm = scope;
    this.lightbox = {
      emptyTrash: false,
    };
  }

  public emptyTrashbin(): void {
    nextcloudService
      .deleteTrash(model.me.userId)
      .then(() => {
        return nextcloudService.listTrash(model.me.userId);
      })
      .then((syncDocuments) => {
        nextcloudEventService.sendDocuments({
          parentDocument: null,
          documents: syncDocuments,
        });
        this.lightbox.emptyTrash = false;
        safeApply(this.vm);
      })
      .catch((err: Error) => {
        const message: string = "Error while attempting to empty trashbin ";
        console.error(message + err.message);
      });
  }
}
