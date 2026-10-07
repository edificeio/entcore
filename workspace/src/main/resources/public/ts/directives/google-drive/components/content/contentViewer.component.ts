import { AxiosError } from "axios";
import { angular, idiom as lang, Me, model, ng, template, toasts } from "entcore";
import { Subscription } from "rxjs";
import { ViewMode } from "../../enums/viewMode.enum";
import { Draggable } from "../../models/googleDriveDraggable.model";
import { GoogleDriveDocument } from "../../models/googleDriveDocument.model";
import { GOOGLE_DRIVE_CREATE_DOCUMENT_TYPES } from "../../models/googleDriveCreateDocumentType.model";
import { IGoogleDriveEventService } from "../../services/googleDriveEvent.service";
import {
  GoogleDrivePreference,
  Preference,
} from "../../services/googleDrive.preferences";
import { IGoogleDriveService } from "../../services/googleDrive.service";
import { safeApply } from "../../utils/safeApply.utils";
import { GoogleDriveViewIcons } from "./iconView.component";
import { GoogleDriveViewList } from "./listView.component";
import { ToolbarSnipletViewModel } from "./toolbar.component";

declare let window: any;

const googleDriveTree: string = "google-drive-folder-tree";

export interface IWorkspaceGoogleDriveContent {
  safeApply(): void;
  initDraggable(): void;
  onSelectContent(document: GoogleDriveDocument): void;
  onSelectAll(): void;
  onOpenContent(document: GoogleDriveDocument): void;
  viewFile: GoogleDriveDocument | null;
  getFile(document: GoogleDriveDocument): string;
  getFilePreview(document: GoogleDriveDocument): string;
  openEditor(document: GoogleDriveDocument): void;
  openLocation(document: GoogleDriveDocument): void;
  draggable: Draggable;
  lockDropzone: boolean;
  isImporting: boolean;
  isMoving: boolean;
  isDeleting: boolean;
  isRestoring: boolean;
  parentDocument: GoogleDriveDocument;
  documents: Array<GoogleDriveDocument>;
  selectedDocuments: Array<GoogleDriveDocument>;
  checkboxSelectAll: boolean;
  moveDocument(element: any, document: GoogleDriveDocument): Promise<void>;
  isDropzoneEnabled(): boolean;
  canDropOnFolder(): boolean;
  onCannotDropFile(): void;
  isViewMode(mode: ViewMode): boolean;
  changeViewMode(mode: ViewMode): Promise<void>;
  isLoaded: boolean;
  viewIcons: GoogleDriveViewIcons;
  viewList: GoogleDriveViewList;
  toolbar: ToolbarSnipletViewModel;
  updateTree(): void;
  getGoogleDriveTreeController(): any;
  isTrashMode(): boolean;
  isSharedMode(): boolean;
  canShowOwner(content?: GoogleDriveDocument): boolean;
  canShowOwnerColumn(): boolean;
  isImportableFolder(): boolean;
  isTrashEmptyable(): boolean;
  triggerCreateFolder(): void;
  triggerEmptyTrash(): void;
  googleDriveDocTypes: { label: string, icon: string, type: string }[];
  isGoogleDriveCreateDocumentMenuOpen: boolean;
  toggleGoogleDriveCreateDocumentMenu(): void;
  triggerGoogleDriveCreateDocument(docType: { type: string }): void;
  translate(key: string): string;
  openDocument(document?: GoogleDriveDocument): any;
  closeViewFile(): void;
  triggerImportFiles(): void;
  footerBackgroundColor: string;
  footerLeft: number;
  footerWidth: number;
  contentAreaHeight: number;
  breadcrumb: Array<IGoogleDriveBreadcrumbEntry>;
  goToBreadcrumb(entry: IGoogleDriveBreadcrumbEntry): void;
  openTileMenuFor: GoogleDriveDocument | null;
  tileMenuPosition: { top?: string; right?: string };
  isTileMenuOpen(content: GoogleDriveDocument): boolean;
  toggleTileMenu(content: GoogleDriveDocument, $event?: MouseEvent): void;
  onTileOpen(content: GoogleDriveDocument): void;
  onTileEdit(content: GoogleDriveDocument): void;
  onTileDownload(content: GoogleDriveDocument): void;
  onTileRename(content: GoogleDriveDocument): void;
  onTileMove(content: GoogleDriveDocument): void;
  onTileCopy(content: GoogleDriveDocument): void;
  onTileDuplicate(content: GoogleDriveDocument): void;
  onTileShare(content: GoogleDriveDocument): void;
  onTileDelete(content: GoogleDriveDocument): void;
  onTileRestore(content: GoogleDriveDocument): void;
  refreshCurrentFolder(): void;
}

export interface IGoogleDriveBreadcrumbEntry {
  name: string;
  folder: GoogleDriveDocument;
}

// Samples the sidebar's rendered background instead of hardcoding a hex, since themes vary.
function getSidebarBackgroundColor(): string {
  let el: Element | null = document.querySelector(
    "nav.vertical.nav-droppable.mobile-navigation",
  );
  while (el) {
    const bg = window.getComputedStyle(el).backgroundColor;
    if (bg && bg !== "rgba(0, 0, 0, 0)" && bg !== "transparent") {
      return bg;
    }
    el = el.parentElement;
  }
  return "transparent";
}

// Footer must align with the sidebar's actual rendered box; a hardcoded left:0 isn't safe to assume.
function getSidebarRect(): { left: number; width: number } {
  const sidebarEl = document.querySelector("nav.vertical.nav-droppable.mobile-navigation");
  if (!sidebarEl) return { left: 0, width: 0 };
  const rect = sidebarEl.getBoundingClientRect();
  return { left: rect.left, width: rect.width };
}

// Theme's own calc(100vh - Npx) height assumes a taller header, so compute it from the box's actual top instead.
const CONTENT_BOTTOM_GAP_PX = 10;
function getContentAreaHeight(): number {
  const boxEl = document.querySelector(".list-view, .icons-view.google-drive-article, .embedded-viewer");
  if (!boxEl) return 0;
  const top = boxEl.getBoundingClientRect().top;
  return Math.max(0, window.innerHeight - top - CONTENT_BOTTOM_GAP_PX);
}

export const workspaceGoogleDriveContentController = ng.controller(
  "GoogleDriveContentController",
  [
    "$scope",
    "GoogleDriveService",
    "GoogleDriveEventService",
    (
      $scope: IWorkspaceGoogleDriveContent,
      googleDriveService: IGoogleDriveService,
      googleDriveEventService: IGoogleDriveEventService,
    ) => {
      $scope.isLoaded = false;
      $scope.documents = [];
      $scope.parentDocument = null;
      $scope.selectedDocuments = [];
      $scope.footerBackgroundColor = getSidebarBackgroundColor();
      $scope.footerLeft = 0;
      $scope.footerWidth = 0;
      $scope.contentAreaHeight = 0;
      $scope.breadcrumb = [];

      // No stable folder tree to walk for ancestors, so breadcrumb is inferred from navigation events.
      let breadcrumbState: Array<IGoogleDriveBreadcrumbEntry> = [];
      const computeBreadcrumb = (
        previousParent: GoogleDriveDocument,
        previousDocuments: Array<GoogleDriveDocument>,
        nextParent: GoogleDriveDocument,
      ): Array<IGoogleDriveBreadcrumbEntry> => {
        if (nextParent && !nextParent.isFolder) {
          // A file, not a folder — never a valid breadcrumb crumb; leave the folder breadcrumb as-is.
          return breadcrumbState;
        }
        if (!nextParent || nextParent.id === null || nextParent.isGoogleDriveParent) {
          // Root's .name is a fresh "Google Drive" instance, not the sidebar's renamed "Mes documents" — reuse the sidebar's node instead.
          const myDrive: GoogleDriveDocument =
            $scope.getGoogleDriveTreeController()?.documents?.[0] ?? nextParent;
          breadcrumbState = [{ name: lang.translate("google-drive.mydrive"), folder: myDrive }];
        } else if (nextParent.isStaticFolder) {
          breadcrumbState = [{ name: nextParent.name, folder: nextParent }];
        } else if (previousDocuments?.some((doc) => doc.id === nextParent.id)) {
          breadcrumbState = [...breadcrumbState, { name: nextParent.name, folder: nextParent }];
        } else if (previousParent?.id === nextParent.id) {
          // keep existing breadcrumb as-is
        } else {
          // Unknown jump — can't reconstruct full ancestry, fall back to a shallow path.
          const myDrive: GoogleDriveDocument =
            $scope.getGoogleDriveTreeController()?.documents?.[0] ?? nextParent;
          breadcrumbState = [
            { name: lang.translate("google-drive.mydrive"), folder: myDrive },
            { name: nextParent.name, folder: nextParent },
          ];
        }
        return breadcrumbState;
      };

      $scope.goToBreadcrumb = function (entry: IGoogleDriveBreadcrumbEntry): void {
        if (!entry?.folder) return;
        googleDriveEventService.sendOpenFolderDocument(entry.folder);
      };

      const recomputeFooterOffsets = (): void => {
        const sidebarRect = getSidebarRect();
        $scope.footerLeft = sidebarRect.left;
        $scope.footerWidth = sidebarRect.width;
        $scope.contentAreaHeight = getContentAreaHeight();
        safeApply($scope);
      };
      // A single deferred call isn't reliable — the new template may not be compiled yet, so retry with backoff.
      const scheduleRecompute = (attempt: number = 0): void => {
        const boxEl = document.querySelector(".list-view, .icons-view.google-drive-article, .embedded-viewer");
        if (boxEl || attempt >= 10) {
          recomputeFooterOffsets();
          return;
        }
        setTimeout(() => scheduleRecompute(attempt + 1), 50);
      };
      window.addEventListener("resize", recomputeFooterOffsets);
      ($scope as any).$on("$destroy", () => {
        window.removeEventListener("resize", recomputeFooterOffsets);
      });

      let googleDrivePreference = new Preference();
      let subscription = new Subscription();

      $scope.getGoogleDriveTreeController = function () {
        return angular.element(document.getElementById(googleDriveTree)).scope();
      };

      $scope.isTrashMode = function (): boolean {
        return $scope.getGoogleDriveTreeController()?.isTrashbinOpen ?? false;
      };

      $scope.isSharedMode = function (): boolean {
        return $scope.getGoogleDriveTreeController()?.isSharedViewOpen ?? false;
      };

      // Always shown in "Partagé avec moi" (whoever shared it with you); outside it, only shown for a
      // file you don't own (e.g. added by an editor on a folder you shared) — explains at a glance why
      // actions like "Placer dans la corbeille" are unavailable for it.
      $scope.canShowOwner = function (content?: GoogleDriveDocument): boolean {
        return $scope.isSharedMode() || (content != null && !content.ownedByMe);
      };

      // List view's owner column can't appear per-row (breaks table alignment) — show the whole
      // column as soon as it'd be relevant for at least one row.
      $scope.canShowOwnerColumn = function (): boolean {
        return $scope.isSharedMode() || ($scope.documents ?? []).some((doc) => !doc.ownedByMe);
      };

      // Mobile-only mirror of the header's "Créer un dossier"/"Vider la corbeille" actions.
      // Importing into "Partagé avec moi" itself (the flat top-level list) makes no sense — there's no
      // real folder there — but a specific shared SUBfolder with editor access is a legitimate upload
      // target: Drive's upload API has no ownership check, only the (here, satisfied) access check.
      $scope.isImportableFolder = function (): boolean {
        if ($scope.isTrashMode()) return false;
        if (!$scope.isSharedMode()) return true;
        return !$scope.parentDocument?.isStaticFolder && $scope.parentDocument?.permissionRole !== "reader";
      };

      $scope.isTrashEmptyable = function (): boolean {
        return ($scope.getGoogleDriveTreeController()?.documents?.length ?? 0) > 0;
      };

      $scope.triggerCreateFolder = function (): void {
        $scope.getGoogleDriveTreeController()?.folderCreation?.toggleCreateFolder(true, null);
      };

      $scope.triggerEmptyTrash = function (): void {
        const treeScope = $scope.getGoogleDriveTreeController();
        if (treeScope?.emptyTrashbin) treeScope.emptyTrashbin.lightbox.emptyTrash = true;
      };

      // Mobile "Créer un document" dropdown — mirrors the header button in controller.ts (separate scope).
      $scope.googleDriveDocTypes = GOOGLE_DRIVE_CREATE_DOCUMENT_TYPES;
      $scope.isGoogleDriveCreateDocumentMenuOpen = false;
      $scope.toggleGoogleDriveCreateDocumentMenu = function (): void {
        $scope.isGoogleDriveCreateDocumentMenuOpen = !$scope.isGoogleDriveCreateDocumentMenuOpen;
      };
      $scope.triggerGoogleDriveCreateDocument = function (docType: { type: string }): void {
        $scope.isGoogleDriveCreateDocumentMenuOpen = false;
        // parentDocument.id is null at Drive root (initParent()), so the backend keeps its default behavior there.
        const parentId = $scope.parentDocument?.id;
        const url = `/googledrive/files/create/${docType.type}` + (parentId ? `?parentId=${encodeURIComponent(parentId)}` : "");
        window.open(url, "_blank");
      };
      $scope.translate = function (key: string): string {
        return lang.translate(key);
      };
      // Close the dropdown on outside click (separate scope from controller.ts's own listener).
      document.addEventListener("click", function (event: MouseEvent): void {
        if (!$scope.isGoogleDriveCreateDocumentMenuOpen) return;
        if ((event.target as HTMLElement)?.closest(".google-drive-create-document-wrapper")) return;
        $scope.isGoogleDriveCreateDocumentMenuOpen = false;
        safeApply($scope);
      });

      // Per-tile "..." menu (icon view) — acts on the tile's own document without going through the
      // real selection state, so opening it never visually selects the tile.
      $scope.openTileMenuFor = null;
      $scope.tileMenuPosition = {};
      $scope.isTileMenuOpen = function (content: GoogleDriveDocument): boolean {
        return $scope.openTileMenuFor === content;
      };
      $scope.toggleTileMenu = function (content: GoogleDriveDocument, $event?: MouseEvent): void {
        const wasOpen = $scope.isTileMenuOpen(content);
        $scope.openTileMenuFor = wasOpen ? null : content;
        if (!wasOpen && $event) {
          const rect = ($event.currentTarget as HTMLElement).getBoundingClientRect();
          $scope.tileMenuPosition = {
            top: (rect.bottom + 4) + "px",
            right: (window.innerWidth - rect.right) + "px",
          };
        }
      };
      document.addEventListener("click", function (event: MouseEvent): void {
        if (!$scope.openTileMenuFor) return;
        if ((event.target as HTMLElement)?.closest(".tile-menu-wrapper")) return;
        $scope.openTileMenuFor = null;
        safeApply($scope);
      });
      // position:fixed menus don't follow their trigger when an ancestor scrolls underneath them —
      // close instead of leaving a stale-positioned dropdown floating disconnected from its tile.
      document.addEventListener("scroll", function (): void {
        if (!$scope.openTileMenuFor) return;
        $scope.openTileMenuFor = null;
        safeApply($scope);
      }, true);
      $scope.onTileOpen = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        $scope.onOpenContent(content);
      };
      $scope.onTileEdit = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        googleDriveService.openEditLink(model.me.userId, content);
      };
      $scope.onTileDownload = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        $scope.toolbar.downloadFiles([content]);
      };
      $scope.onTileRename = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        // renameDocument() (toolbar.component.ts) reads this.vm.selectedDocuments[0] to know which
        // file to rename — unlike move/copy/share, it isn't closure-captured from the array passed
        // to toggleRenameView(), so it must be set here too (same as onTileDelete/onTileRestore).
        $scope.selectedDocuments = [content];
        $scope.toolbar.toggleRenameView(true, [content]);
      };
      $scope.onTileMove = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        $scope.toolbar.toggleMoveView(true, [content]);
      };
      $scope.onTileCopy = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        $scope.toolbar.toggleCopyView(true, [content]);
      };
      $scope.onTileDuplicate = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        $scope.toolbar.toggleDuplicateView(true, [content]);
      };
      $scope.onTileShare = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        $scope.toolbar.share.toggleShareView(true, [content]);
      };
      $scope.onTileDelete = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        $scope.selectedDocuments = [content];
        $scope.toolbar.toggleDeleteView(true);
      };
      $scope.onTileRestore = function (content: GoogleDriveDocument): void {
        $scope.openTileMenuFor = null;
        $scope.selectedDocuments = [content];
        $scope.toolbar.restoreDocuments();
      };
      // Re-fetches the current folder from Google Drive — the same event the tree uses after its
      // own actions (move/delete/etc), so edits made outside the app (directly in Drive) show up
      // without a full page reload.
      $scope.refreshCurrentFolder = function (): void {
        googleDriveEventService.sendOpenFolderDocument($scope.parentDocument);
      };

      Promise.all([
        initDocumentsContent(googleDriveService, $scope),
        googleDrivePreference.init(),
      ])
        .then(async () => {
          await $scope.changeViewMode(googleDrivePreference.viewMode);
          $scope.viewList = new GoogleDriveViewList($scope);
          $scope.viewIcons = new GoogleDriveViewIcons($scope);
          $scope.toolbar = new ToolbarSnipletViewModel($scope);
          $scope.isLoaded = true;
          safeApply($scope);
          scheduleRecompute();
        })
        .catch((err: AxiosError) => {
          console.error("Error while initializing Google Drive content: " + err.message);
          $scope.isLoaded = true;
          safeApply($scope);
        });

      subscription.add(
        googleDriveEventService
          .getDocumentsState()
          .subscribe(
            (res: { parentDocument: GoogleDriveDocument; documents: Array<GoogleDriveDocument> }) => {
              $scope.breadcrumb = computeBreadcrumb($scope.parentDocument, $scope.documents, res.parentDocument);

              // Navigating away while a file is open in the viewer needs to switch back to the list/icon view.
              if ($scope.viewFile) {
                $scope.viewFile = null;
                const preference: GoogleDrivePreference = Me.preferences["google-drive"];
                $scope.changeViewMode(preference.viewMode);
              }

              if (res.documents && res.documents.length > 0) {
                $scope.parentDocument = res.parentDocument;
                if ($scope.isTrashMode()) {
                  $scope.documents = res.documents.sort(sortDocumentsByFolder);
                } else {
                  $scope.documents = res.documents.sort(sortDocumentsByFolder);
                }
              } else {
                $scope.parentDocument = res.parentDocument;
                $scope.documents = [];
              }
              // $scope.documents is a fresh array on every navigation, but $scope.selectedDocuments
              // (which drives the bottom action toolbar) is a separate property only updated by
              // onSelectContent — without this it keeps stale references after switching folders.
              $scope.selectedDocuments = [];
              $scope.isLoaded = true;
              safeApply($scope);
              // Breadcrumb/footer height can change, so recompute the content area's bottom edge.
              scheduleRecompute();
            },
          ),
      );

      ($scope as any).$on("$destroy", () => subscription.unsubscribe());

      initDraggable();

      async function initDocumentsContent(
        service: IGoogleDriveService,
        scope: IWorkspaceGoogleDriveContent,
      ): Promise<void> {
        if ($scope.isTrashMode() || $scope.isSharedMode()) return;

        const selectedFolder: GoogleDriveDocument =
          $scope.getGoogleDriveTreeController()?.["selectedFolder"];
        const parentId = selectedFolder?.id ?? null;

        return service
          .listDocument(model.me.userId, parentId)
          .then((documents: Array<GoogleDriveDocument>) => {
            if (!scope.documents.length) {
              scope.documents = documents
                .filter((doc) => !parentId || doc.id !== parentId)
                .sort(sortDocumentsByFolder);
              scope.parentDocument = new GoogleDriveDocument().initParent();
            }
            safeApply(scope);
          })
          .catch((err: AxiosError) => {
            console.error("Error while fetching Google Drive documents: " + err.message);
          });
      }

      function initDraggable(): void {
        const viewModel = $scope;
        let dropTarget: Element | null = null;

        // MutationObserver: set draggable="true" on any element that has the entcore
        // dragstart directive attribute as soon as ng-repeat adds it to the DOM.
        // This bypasses the entcore dragcondition evaluation and browser template caching.
        const contentEl = document.getElementById("google-drive-content");
        if (contentEl) {
          const observer = new MutationObserver(() => {
            contentEl.querySelectorAll("[dragstart]").forEach((el: Element) => {
              if (el.getAttribute("draggable") !== "true") {
                el.setAttribute("draggable", "true");
              }
            });
          });
          observer.observe(contentEl, { childList: true, subtree: true });

          // dragstart/dragend don't bubble — when the drag source is an inner element
          // (e.g., the <div class="element"> inside <explorer> in icons view), the jQuery
          // handlers registered by the entcore directive on the outer [dragstart]/[dragend]
          // element never fire. Capture-phase listeners on contentEl fire first and walk up
          // to the nearest ancestor that carries our handler.
          const onCaptureDragStart = (e: DragEvent): void => {
            const target = e.target as Element;
            const hasDragstartAttr = target.getAttribute?.("dragstart");
            const dragEl = target.closest?.("[dragstart]");
            if (hasDragstartAttr) return;
            if (!dragEl) return;
            // explorer uses isolated scope — content is on $parent (ng-repeat scope)
            const scope: any = angular.element(dragEl).scope();
            const content = scope?.content ?? scope?.$parent?.content ?? scope?.document ?? null;
            if (content !== null && content !== undefined) {
              viewModel.draggable.dragStartHandler(e, content);
            }
          };
          const onCaptureDragEnd = (e: DragEvent): void => {
            const target = e.target as Element;
            if (target.getAttribute?.("dragend")) return; // jQuery handler will fire
            const dragEl = target.closest?.("[dragend]");
            if (!dragEl) return;
            // explorer uses isolated scope — content is on $parent (ng-repeat scope)
            const scope: any = angular.element(dragEl).scope();
            const content = scope?.content ?? scope?.$parent?.content ?? scope?.document ?? null;
            if (content !== null && content !== undefined) {
              viewModel.draggable.dragEndHandler(e, content);
            }
          };
          contentEl.addEventListener("dragstart", onCaptureDragStart, true);
          contentEl.addEventListener("dragend", onCaptureDragEnd, true);

          ($scope as any).$on("$destroy", () => {
            observer.disconnect();
            contentEl.removeEventListener("dragstart", onCaptureDragStart, true);
            contentEl.removeEventListener("dragend", onCaptureDragEnd, true);
          });
        }

        // The entcore dragdrop directive calls stopPropagation on dragover,
        // so we cannot track the target via document dragover. Instead we
        // capture the drop event (which does bubble) to get the real target.
        const onNativeDrop = (e: Event): void => {
          dropTarget = e.target as Element;
        };

        $scope.draggable = {
          dragConditionHandler(event: DragEvent, content?: any): boolean {
            return false;
          },
          dragDropHandler(event: DragEvent, content?: any): void {},
          async dragEndHandler(event: DragEvent, content?: any): Promise<void> {
            // Defensive reset: a stray inline position (e.g. left by a browser quirk or an
            // unrelated legacy drag plugin picking up the "draggable" attribute) would otherwise
            // leave the tile visually stuck instead of snapping back to its grid position.
            const draggedEl = event?.target as HTMLElement;
            if (draggedEl?.style) {
              draggedEl.style.position = "";
              draggedEl.style.top = "";
              draggedEl.style.left = "";
              draggedEl.style.transition = "";
            }
            document.removeEventListener("drop", onNativeDrop);
            // Skip moveDocument if drop was on the GD folder tree — capture-phase handler already handled it
            if (dropTarget && !dropTarget.closest?.("#google-drive-folder-tree")) {
              await viewModel.moveDocument(dropTarget, content);
            }
            dropTarget = null;
            // Clear contentContext: if drop was handled, it's already null; if drag was
            // cancelled or dropped on an invalid target, this prevents stale context.
            googleDriveEventService.setContentContext(null);
            viewModel.lockDropzone = false;
            safeApply($scope);
          },
          dragStartHandler(event: DragEvent, content?: any): void {
            // "Partagé avec moi" items have no "Exporter" action either (not owned, can't be copied
            // to another drive) — dragging onto the sidebar tree's other drives must be blocked too.
            if (viewModel.isSharedMode()) {
              event.preventDefault();
              return;
            }
            viewModel.lockDropzone = true;
            dropTarget = null;
            document.addEventListener("drop", onNativeDrop);
            // Serialize only primitive fields: CacheList properties cause circular reference errors
            const transferData = content ? JSON.stringify({
              id: content.id, name: content.name,
              isFolder: content.isFolder, type: content.type,
            }) : "{}";
            event.dataTransfer.setData("application/json", transferData);
            googleDriveEventService.setContentContext(content);
            // Without this, lockDropzone's flip to true isn't applied until some later digest —
            // the OS-file dropzone-overlay (its own dragenter/drop listeners) stays mounted during
            // an internal tile drag and can misfire on drop. Classic space's own drag() already does this.
            safeApply($scope);
          },
          dropConditionHandler(event: DragEvent, content?: any): boolean {
            return true;
          },
        };

        // OS file drop on the content area → upload to the currently open folder.
        // Intercept in capture phase before the entcore dragdrop directive (bubble phase)
        // tries JSON.parse on empty dataTransfer data and crashes.
        const onOsFileDragOverContent = (e: DragEvent): void => {
          const target = e.target as Element;
          if (!target.closest?.("#google-drive-content")) return;
          if (!Array.from(e.dataTransfer?.types ?? []).includes("Files")) return;
          e.preventDefault();
          if (e.dataTransfer) e.dataTransfer.dropEffect = "copy";
        };
        const uploadFilesToCurrentFolder = (allFiles: Array<File>): void => {
          if (allFiles.length === 0) return;
          // Same limit/message as the classic workspace's own import dialog (external entcore
          // package, "max.file.size" i18n key) — checked there only AFTER the server has received
          // the whole file (413), which is also how the 499 memory-exhaustion crash happens for a
          // huge upload: checking client-side first means we never even attempt sending it.
          const maxFileSize = parseInt(lang.translate("max.file.size"), 10);
          const files = allFiles.filter((f) => !maxFileSize || f.size <= maxFileSize);
          if (files.length < allFiles.length) {
            toasts.warning(
              lang.translate("file.too.large.limit") +
                Math.round(maxFileSize / 1024 / 1024) +
                lang.translate("mb"),
            );
          }
          if (files.length === 0) return;
          // Setting lockDropzone=true removes <dropzone-overlay> from the DOM via ng-if.
          // When it re-enters the DOM (lockDropzone=false), the directive re-links and
          // calls scope.hide() so it starts invisible.
          viewModel.lockDropzone = true;
          viewModel.isImporting = true;
          safeApply($scope);
          const targetFolder = viewModel.parentDocument ?? null;
          const done = (): void => {
            viewModel.lockDropzone = false;
            viewModel.isImporting = false;
            safeApply($scope);
          };
          googleDriveService
            .uploadLocalFilesToCloud(model.me.userId, files, targetFolder?.id ?? undefined)
            .then(() => {
              googleDriveEventService.sendOpenFolderDocument(
                targetFolder ?? new GoogleDriveDocument().initParent(),
              );
            })
            .catch((err: AxiosError) => {
              console.error("Error uploading local files to Google Drive: " + err.message);
              // 499 ("client closed request"): the whole file is buffered in memory server-side
              // (see DefaultDocumentsService's storage.readFile/.getBytes()), so a very large upload
              // can exhaust the JVM heap and drop the connection well before any real size cap.
              if (err.response?.status === 499) {
                toasts.warning("google-drive.upload.too.large");
              } else {
                toasts.warning("google-drive.upload.error");
              }
            })
            .then(done, done);
        };

        const onOsFileDropContent = (e: DragEvent): void => {
          const target = e.target as Element;
          if (!target.closest?.("#google-drive-content")) return;
          if (!Array.from(e.dataTransfer?.types ?? []).includes("Files")) return;
          e.stopPropagation();
          e.preventDefault();
          uploadFilesToCurrentFolder(Array.from(e.dataTransfer?.files ?? []));
        };
        document.addEventListener("dragover", onOsFileDragOverContent, true);
        document.addEventListener("drop", onOsFileDropContent, true);
        ($scope as any).$on("$destroy", () => {
          document.removeEventListener("dragover", onOsFileDragOverContent, true);
          document.removeEventListener("drop", onOsFileDropContent, true);
        });

        const onImportInputChange = (e: Event): void => {
          const input = e.target as HTMLInputElement;
          uploadFilesToCurrentFolder(Array.from(input.files ?? []));
          input.value = "";
        };
        const importInput = document.getElementById("google-drive-import-input");
        importInput?.addEventListener("change", onImportInputChange);
        ($scope as any).$on("$destroy", () => {
          importInput?.removeEventListener("change", onImportInputChange);
        });
      }

      $scope.triggerImportFiles = function (): void {
        // Deferred: calling .click() synchronously here bubbles a native click event up to
        // document.body while we're still mid-digest (this is itself invoked from an ng-click),
        // and an unrelated body-level click listener calls $apply() without a $$phase guard —
        // throwing "$apply already in progress" and leaving the digest broken, so the import
        // spinner (and basically all further scope updates) silently stop appearing.
        setTimeout(() => {
          document.getElementById("google-drive-import-input")?.click();
        }, 0);
      };

      function sortDocumentsByFolder(
        a: GoogleDriveDocument,
        b: GoogleDriveDocument,
      ): number {
        if (a.isFolder && !b.isFolder) return -1;
        if (!a.isFolder && b.isFolder) return 1;
        return 0;
      }

      // Walk up from the drop target to find a content-grid tile's scope holding a GD folder.
      // Mirrors googleDriveFolder.directive.ts's own findFolderScope() for the sidebar tree,
      // except tile scopes expose "content" (ng-repeat="content in documents" in icons.html/
      // list.html), not "folder" — that property name is a sidebar-tree-only convention.
      function findTileFolderScope(target: Element): any {
        let el: Element | null = target;
        const contentEl = document.getElementById("google-drive-content");
        while (el && el !== contentEl) {
          const s: any = angular.element(el).scope();
          if (s?.content instanceof GoogleDriveDocument && s.content.isFolder) return s;
          el = el.parentElement;
        }
        return null;
      }

      $scope.moveDocument = async function (
        element: any,
        document: GoogleDriveDocument,
      ): Promise<void> {
        let selectedFolder: GoogleDriveDocument =
          $scope.getGoogleDriveTreeController()?.["selectedFolder"];
        if (!selectedFolder) selectedFolder = $scope.parentDocument;

        const folderContent: any = findTileFolderScope(element);
        if (folderContent?.content instanceof GoogleDriveDocument && folderContent.content.isFolder) {
          const targetFolder: GoogleDriveDocument = folderContent.content;
          const filesToMove = new Set($scope.selectedDocuments);
          filesToMove.add(document);
          const promises = Array.from(filesToMove)
            .filter((doc) => doc.id !== targetFolder.id)
            .map((doc) =>
              googleDriveService.moveDocument(model.me.userId, doc.id, targetFolder.id),
            );
          $scope.isMoving = true;
          safeApply($scope);
          const done = (): void => {
            $scope.isMoving = false;
            safeApply($scope);
          };
          Promise.all(promises)
            .then(() => refreshDocList(selectedFolder))
            .catch((err: AxiosError) => {
              console.error("Error while moving document: " + err.message);
              return refreshDocList(selectedFolder);
            })
            .then(done, done);
        }
      };

      function refreshDocList(selectedFolder: GoogleDriveDocument): Promise<void> {
        $scope.selectedDocuments = [];
        return googleDriveService
          .listDocument(model.me.userId, selectedFolder?.id || null)
          .then((docs) => {
            $scope.documents = docs
              .filter((doc) => doc.id !== selectedFolder?.id)
              .sort((a, b) => (a.isFolder && !b.isFolder ? -1 : !a.isFolder && b.isFolder ? 1 : 0));
            googleDriveEventService.setContentContext(null);
            googleDriveEventService.sendOpenFolderDocument(selectedFolder);
            safeApply($scope);
          })
          .catch((err: AxiosError) => {
            console.error("Error refreshing document list: " + err.message);
          });
      }

      $scope.onSelectContent = function (content: GoogleDriveDocument): void {
        $scope.selectedDocuments = $scope.documents.filter((doc) => doc.selected);
      };

      $scope.onSelectAll = function (): void {
        $scope.checkboxSelectAll = !$scope.checkboxSelectAll;
        $scope.documents.forEach((doc) => (doc.selected = $scope.checkboxSelectAll));
        $scope.selectedDocuments = $scope.documents.filter((doc) => doc.selected);
      };

      $scope.isViewMode = function (mode: ViewMode): boolean {
        return template.contains("documents-content", `google-drive/content/views/${mode}`);
      };

      $scope.changeViewMode = async function (mode: ViewMode): Promise<void> {
        let preference: GoogleDrivePreference = Me.preferences["google-drive"];
        preference.viewMode = mode;
        await googleDrivePreference.updatePreference(preference);
        $scope.documents.forEach((doc) => (doc.selected = false));
        $scope.selectedDocuments = [];
        template.open("documents-content", `google-drive/content/views/${mode}`);
        safeApply($scope);
        scheduleRecompute();
      };

      $scope.openDocument = function (document?: GoogleDriveDocument): any {
        $scope.viewFile = document ?? $scope.selectedDocuments[0];
        template.open("documents-content", `google-drive/content/views/viewer`);
        $scope.selectedDocuments = [];
        scheduleRecompute();
      };

      $scope.closeViewFile = function (): any {
        const preference: GoogleDrivePreference = Me.preferences["google-drive"];
        $scope.viewFile = null;
        $scope.changeViewMode(preference.viewMode);
      };

      $scope.onOpenContent = function (document: GoogleDriveDocument): void {
        if (document.isFolder) {
          googleDriveEventService.sendOpenFolderDocument(document);
          $scope.selectedDocuments = [];
        } else {
          $scope.openDocument(document);
        }
      };

      $scope.getFile = function (document: GoogleDriveDocument): string {
        if (!document) return "";
        return googleDriveService.getFile(model.me.userId, document.id, document.isFolder);
      };

      $scope.getFilePreview = function (document: GoogleDriveDocument): string {
        if (!document) return "";
        return googleDriveService.getFilePreview(model.me.userId, document.id);
      };

      $scope.openEditor = function (document: GoogleDriveDocument): void {
        googleDriveService.openEditLink(model.me.userId, document);
      };

      $scope.openLocation = function (document: GoogleDriveDocument): void {
        googleDriveService.openLocationLink(model.me.userId, document);
      };

      $scope.isDropzoneEnabled = function (): boolean {
        return !$scope.lockDropzone;
      };

      $scope.canDropOnFolder = function (): boolean {
        return true;
      };

      $scope.onCannotDropFile = function (): void {};

      $scope.safeApply = function (): void {
        safeApply($scope);
      };
    },
  ],
);

export const workspaceGoogleDriveContent = ng.directive(
  "workspaceGoogleDriveContent",
  () => {
    return {
      restrict: "E",
      templateUrl:
        "/workspace/public/template/google-drive/content/workspace-google-drive-content.html",
    };
  },
);
