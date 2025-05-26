import { setTimeout } from "core-js";
import {
  idiom as lang,
  model,
  notify,
  SharePayload,
  template,
  workspace,
} from "entcore";
import { SyncDocument } from "../../models/nextcloudFolder.model";
import { nextcloudService } from "../../services/nextcloud.service";
import { WorkspaceEntcoreUtils } from "../../utils/workspaceEntcore.utils";
import models = workspace.v2.models;

interface IViewModel {
  copyingForShare: boolean;

  sharedElement: Array<any>;
  toggleShareView(
    state: boolean,
    selectedDocuments?: Array<SyncDocument>,
  ): void;
  onShareAndNotCopy(): void;
  onShareAndCopy(): void;

  onSubmitSharedElements(share: SharePayload): Promise<void>;
  onCancelShareElements(): Promise<void>;
}

export class ToolbarShareSnipletViewModel implements IViewModel {
  private vm: any;
  private lightbox: any;

  copyingForShare: boolean;
  sharedElement: Array<any>;

  constructor(scopeParent: any, lightbox: any) {
    this.vm = scopeParent;
    this.lightbox = lightbox;
    this.sharedElement = [];
  }

  toggleShareView(
    state: boolean,
    selectedDocuments?: Array<SyncDocument>,
  ): void {
    this.lightbox.share = state;
    if (state && selectedDocuments) {
      this.copyingForShare = false;
      const pathTemplate: string = `nextcloud/toolbar/share/share-documents-options`;
      template.open("workspace-nextcloud-toolbar-share", pathTemplate);
    } else {
      template.close("workspace-nextcloud-toolbar-share");
      this.copyingForShare = true;
    }
  }

  onShareAndNotCopy(): void {
    const paths: Array<string> = this.vm.selectedDocuments.map(
      (document: SyncDocument) => document.path,
    );
    this.vm.nextcloudService
      .moveDocumentNextcloudToWorkspace(model.me.userId, paths)
      .then(async (workspaceDocuments: Array<models.Element>) => {
        this.sharedElement = workspaceDocuments;
        this.vm.updateTree();
        const pathTemplate: string = `nextcloud/toolbar/share/share`;
        this.vm.selectedDocuments = [];
        template.open("workspace-nextcloud-toolbar-share", pathTemplate);
        try {
          this.vm.getNextcloudTreeController().userInfo = await this.vm
            .getNextcloudTreeController()
            .nextcloudUserService.getUserInfo(model.me.userId);
        } catch (e) {
          notify.error(lang.translate("error.user.info"));
          console.error(e);
        }
        this.vm.safeApply();
      });
  }

  onShareAndCopy(): void {
    const paths: Array<string> = this.vm.selectedDocuments.map(
      (document: SyncDocument) => document.path,
    );
    nextcloudService
      .copyDocumentToWorkspace(model.me.userId, paths)
      .then((workspaceDocuments: Array<models.Element>) => {
        this.sharedElement = workspaceDocuments;
        const pathTemplate: string = `nextcloud/toolbar/share/share`;
        template.open("workspace-nextcloud-toolbar-share", pathTemplate);
      });
  }

  async onSubmitSharedElements(share: SharePayload): Promise<void> {
    this.toggleShareView(false);
    setTimeout(() => {
      WorkspaceEntcoreUtils.toggleWorkspaceContentDisplay(false);
      this.vm.safeApply();
    }, 500);
    this.sharedElement = [];
  }

  async onCancelShareElements(): Promise<void> {
    if (this.sharedElement.length) {
      try {
        await nextcloudService.moveDocumentWorkspaceToCloud(
          model.me.userId,
          this.sharedElement.map((doc) => doc._id),
          this.vm.parentDocument.path,
        );
        this.vm.updateTree();
        this.vm.safeApply();
      } catch (e) {
        console.error("Error while canceling share: " + e);
      }
      this.sharedElement = [];
    }
    this.toggleShareView(false);
  }
}
