import { model, SharePayload, template, workspace } from "entcore";
import { googleDriveService } from "../../services/googleDrive.service";
import { GoogleDriveDocument } from "../../models/googleDriveDocument.model";
import { workspaceService } from "../../../../services";
import { safeApply } from "../../utils/safeApply.utils";
import models = workspace.v2.models;

export class ToolbarShareGoogleDriveViewModel {
  private vm: any;
  private lightbox: any;

  copyingForShare: boolean = false;
  sharedElement: Array<models.Element> = [];

  constructor(scopeParent: any, lightbox: any) {
    this.vm = scopeParent;
    this.lightbox = lightbox;
  }

  toggleShareView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void {
    this.lightbox.share = state;
    if (state && selectedDocuments) {
      this.copyingForShare = false;
      template.open("workspace-google-drive-toolbar-share", "google-drive/toolbar/share/share-documents-options");
    } else {
      template.close("workspace-google-drive-toolbar-share");
    }
  }

  onShareAndCopy(): void {
    this.copyingForShare = true;
    const ids = this.vm.selectedDocuments.map((doc: GoogleDriveDocument) => doc.id);
    googleDriveService
      .copyDocumentToWorkspace(model.me.userId, ids)
      .then((workspaceDocuments: Array<models.Element>) => {
        this.sharedElement = workspaceDocuments;
        template.open("workspace-google-drive-toolbar-share", "google-drive/toolbar/share/share");
        safeApply(this.vm);
      })
      .catch(() => {
        this.copyingForShare = false;
        safeApply(this.vm);
      });
  }

  async onSubmitSharedElements(_share: SharePayload): Promise<void> {
    this.toggleShareView(false);
    this.sharedElement = [];
  }

  async onCancelShareElements(): Promise<void> {
    if (this.sharedElement.length) {
      try {
        await workspaceService.deleteAll(this.sharedElement);
      } catch (e) {
        console.error("[GoogleDrive] Error deleting workspace copies on share cancel", e);
      }
      this.sharedElement = [];
    }
    this.toggleShareView(false);
  }
}
