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
  // Only set for group results from /communication/visible/search — member count shown next to the name.
  nbUsers?: number;
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

  // Bumped on every findUsers() call so a stale in-flight search response (e.g. still pending when
  // the user clicks a group/user result, or types past it) can't overwrite `found` after the fact.
  private searchToken: number = 0;

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
      this.searchToken++;
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
    this.searchToken++;
    this.found = [];
  }

  findUsers(): void {
    const token = ++this.searchToken;
    const term = this.search;
    if (!term || term.length < 3) {
      this.found = [];
      return;
    }
    const alreadyPicked = new Set<string>([
      ...this.selectedRecipients.map((u) => u.id),
      ...this.currentShares.map((s) => s.userId).filter((id): id is string => !!id),
    ]);

    // Users: the proven /userbook/api/search endpoint (unchanged from before groups were added back).
    const usersRequest = http
      .get(`/userbook/api/search?name=${encodeURIComponent(term)}`)
      .then((res: AxiosResponse) => res.data as Array<IGoogleDriveUserSearchResult>)
      .catch((err: Error) => {
        console.error("[GoogleDrive] Error searching users: " + err.message);
        return [] as Array<IGoogleDriveUserSearchResult>;
      });

    // Groups: Google Drive/Workspace groups aren't provisioned, so a group can't be shared with
    // directly — selecting one instead expands to its member users, see addGroupRecipients().
    // /communication/visible/search also returns non-group entries (ShareBookmark, etc.) whose
    // "type" naming can vary by deployment config, so "nbUsers" (group-only field) is the reliable
    // signal here rather than matching on a specific type string.
    const groupsRequest = http
      .get(`/communication/visible/search?query=${encodeURIComponent(term)}`)
      .then((res: AxiosResponse) =>
        (res.data as Array<IGoogleDriveUserSearchResult>)
          .filter((v) => v.nbUsers !== undefined && v.nbUsers !== null)
          .map((v) => ({ ...v, type: "Group" })),
      )
      .catch((err: Error) => {
        console.error("[GoogleDrive] Error searching groups: " + err.message);
        return [] as Array<IGoogleDriveUserSearchResult>;
      });

    Promise.all([usersRequest, groupsRequest]).then(([users, groups]) => {
      if (token !== this.searchToken) return;
      this.found = [...users, ...groups].filter(
        (u) => !alreadyPicked.has(u.id) && u.id !== model.me.userId,
      );
      safeApply(this.vm);
    });
  }

  addRecipient(user: IGoogleDriveUserSearchResult): void {
    this.searchToken++;
    this.selectedRecipients.push(user);
    this.found = this.found.filter((u) => u.id !== user.id);
    this.search = "";
  }

  // A group can't be shared with directly on Google Drive (groups aren't provisioned there), so
  // expand it into its member users instead and add each one individually.
  addGroupRecipients(group: IGoogleDriveUserSearchResult): void {
    this.searchToken++;
    http
      .get(`/userbook/visible/users/${group.id}`)
      .then((res: AxiosResponse) => {
        const alreadyPicked = new Set<string>([
          ...this.selectedRecipients.map((u) => u.id),
          ...this.currentShares.map((s) => s.userId).filter((id): id is string => !!id),
        ]);
        (res.data as Array<IGoogleDriveUserSearchResult>)
          .filter((u) => u.id && !alreadyPicked.has(u.id) && u.id !== model.me.userId)
          .forEach((u) => this.selectedRecipients.push(u));
        this.found = this.found.filter((u) => u.id !== group.id);
        this.search = "";
        safeApply(this.vm);
      })
      .catch((err: Error) => {
        console.error("[GoogleDrive] Error expanding group members: " + err.message);
      });
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
