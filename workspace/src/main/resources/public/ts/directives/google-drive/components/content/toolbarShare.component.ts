import http, { AxiosResponse } from "axios";
import { model, template, toasts } from "entcore";
import {
  GoogleDriveShareRole,
  IGoogleDriveShareEntry,
  IGoogleDriveShareResult,
  googleDriveService,
} from "../../services/googleDrive.service";
import { GoogleDriveDocument } from "../../models/googleDriveDocument.model";
import { safeApply } from "../../utils/safeApply.utils";

export interface IGoogleDriveUserSearchResult {
  id: string;
  userId?: string;
  displayName: string;
  type?: string;
  photo?: string;
}

export interface IGoogleDriveShareEntryResolved extends IGoogleDriveShareEntry {
  displayName?: string;
}

export interface IGoogleDriveShareError {
  recipient: string;
  message: string;
}

export class ToolbarShareGoogleDriveViewModel {
  private vm: any;
  private lightbox: any;

  selectedDocuments: Array<GoogleDriveDocument> = [];
  role: GoogleDriveShareRole = "reader";
  search: string = "";
  found: Array<IGoogleDriveUserSearchResult> = [];
  selectedRecipients: Array<IGoogleDriveUserSearchResult> = [];
  currentShares: Array<IGoogleDriveShareEntryResolved> = [];
  shareErrors: Array<IGoogleDriveShareError> = [];
  loadingShares: boolean = false;
  sharing: boolean = false;

  constructor(scopeParent: any, lightbox: any) {
    this.vm = scopeParent;
    this.lightbox = lightbox;
  }

  get isSingleDocument(): boolean {
    return this.selectedDocuments.length === 1;
  }

  toggleShareView(state: boolean, selectedDocuments?: Array<GoogleDriveDocument>): void {
    this.lightbox.share = state;
    if (state && selectedDocuments) {
      this.selectedDocuments = selectedDocuments;
      this.role = "reader";
      this.search = "";
      this.found = [];
      this.selectedRecipients = [];
      this.currentShares = [];
      this.shareErrors = [];
      this.sharing = false;
      if (this.isSingleDocument) {
        this.loadCurrentShares();
      }
      template.open("workspace-google-drive-toolbar-share", "google-drive/toolbar/share/share");
    } else {
      this.selectedDocuments = [];
      template.close("workspace-google-drive-toolbar-share");
    }
  }

  private loadCurrentShares(): void {
    this.loadingShares = true;
    googleDriveService
      .listSharedUsers(model.me.userId, this.selectedDocuments[0].id)
      .then((entries: Array<IGoogleDriveShareEntry>) =>
        this.resolveDisplayNames(entries.filter((entry) => entry.userId !== model.me.userId)),
      )
      .then((resolved: Array<IGoogleDriveShareEntryResolved>) => {
        this.currentShares = resolved;
        this.loadingShares = false;
        safeApply(this.vm);
      })
      .catch((err: Error) => {
        console.error("[GoogleDrive] Error loading current shares: " + err.message);
        this.loadingShares = false;
        safeApply(this.vm);
      });
  }

  // ENT users should never see the internal synthetic Google Workspace address
  // (<entUserId>@domain) — resolve it back to a real ENT display name for display.
  private resolveDisplayNames(
    entries: Array<IGoogleDriveShareEntry>,
  ): Promise<Array<IGoogleDriveShareEntryResolved>> {
    return Promise.all(
      entries.map((entry) => {
        if (!entry.userId) return entry as IGoogleDriveShareEntryResolved;
        return http
          .get(`/userbook/api/person?id=${entry.userId}`)
          .then((res: AxiosResponse) => {
            const displayName: string = res.data?.result?.[0]?.displayName;
            return { ...entry, displayName } as IGoogleDriveShareEntryResolved;
          })
          .catch(() => entry as IGoogleDriveShareEntryResolved);
      }),
    );
  }

  clearSearch(): void {
    this.found = [];
  }

  findUsers(): void {
    const term = this.search;
    if (!term || term.length < 3) {
      this.found = [];
      return;
    }
    http
      .get(`/userbook/api/search?name=${encodeURIComponent(term)}`)
      .then((res: AxiosResponse) => {
        const alreadyPicked = new Set<string>([
          ...this.selectedRecipients.map((u) => u.id),
          ...this.currentShares.map((s) => s.userId).filter((id): id is string => !!id),
        ]);
        this.found = (res.data as Array<IGoogleDriveUserSearchResult>).filter(
          (u) => !alreadyPicked.has(u.id),
        );
        safeApply(this.vm);
      })
      .catch((err: Error) => {
        console.error("[GoogleDrive] Error searching users: " + err.message);
      });
  }

  addRecipient(user: IGoogleDriveUserSearchResult): void {
    this.selectedRecipients.push(user);
    this.found = this.found.filter((u) => u.id !== user.id);
    this.search = "";
  }

  removeRecipient(user: IGoogleDriveUserSearchResult): void {
    this.selectedRecipients = this.selectedRecipients.filter((u) => u.id !== user.id);
  }

  hasRecipients(): boolean {
    return this.selectedRecipients.length > 0;
  }

  onShare(): void {
    if (!this.selectedRecipients.length || !this.selectedDocuments.length) return;
    this.sharing = true;
    this.shareErrors = [];
    const recipientsById = new Map<string, IGoogleDriveUserSearchResult>(
      this.selectedRecipients.map((u) => [u.id, u]),
    );
    const userIds: Array<string> = this.selectedRecipients.map((u) => u.id);

    Promise.all(
      this.selectedDocuments.map((doc) =>
        googleDriveService
          .shareDocuments(model.me.userId, doc.id, userIds, this.role)
          .catch((err: Error) => {
            console.error(`[GoogleDrive] Error sharing ${doc.id}: ` + err.message);
            return userIds.map(
              (userId): IGoogleDriveShareResult => ({ userId, status: "error" }),
            );
          }),
      ),
    ).then((results: Array<Array<IGoogleDriveShareResult>>) => {
      const flatResults: Array<IGoogleDriveShareResult> = [].concat(...results);
      const failures: Array<IGoogleDriveShareResult> = flatResults.filter(
        (r) => r.status === "error",
      );
      const hasFailures: boolean = failures.length > 0;
      toasts.info(hasFailures ? "google-drive.share.partial.error" : "google-drive.share.success");

      // Reflect the new share state on the file(s) immediately (same object references
      // as in the folder listing), so the "shared" icon updates without a folder reload.
      this.selectedDocuments.forEach((doc, i) => {
        if (results[i]?.some((r) => r.status === "ok")) doc.isShared = true;
      });

      this.shareErrors = failures.map((f) => ({
        recipient: recipientsById.get(f.userId)?.displayName ?? f.userId,
        message: f.message ?? "",
      }));
      this.selectedRecipients = [];
      this.sharing = false;

      if (hasFailures) {
        // Keep the dialog open so the user can see who succeeded/failed and retry.
        if (this.isSingleDocument) this.loadCurrentShares();
        safeApply(this.vm);
      } else {
        this.toggleShareView(false);
      }
    });
  }

  onRemoveShare(entry: IGoogleDriveShareEntry): void {
    if (!this.isSingleDocument || !entry.userId) return;
    googleDriveService
      .unshareDocument(model.me.userId, this.selectedDocuments[0].id, entry.userId)
      .then(() => {
        this.currentShares = this.currentShares.filter((s) => s.id !== entry.id);
        if (!this.currentShares.length) {
          this.selectedDocuments[0].isShared = false;
        }
        toasts.info("google-drive.unshare.success");
        safeApply(this.vm);
      })
      .catch((err: Error) => {
        console.error("[GoogleDrive] Error unsharing: " + err.message);
        toasts.warning("google-drive.unshare.error");
        safeApply(this.vm);
      });
  }

  close(): void {
    this.toggleShareView(false);
  }
}
