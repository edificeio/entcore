import { AxiosError } from "axios";
import { angular, Me, model, ng, template } from "entcore";
import { Subscription } from "rxjs";
import { ViewMode } from "../../enums/viewMode.enum";
import { Draggable } from "../../models/googleDriveDraggable.model";
import { GoogleDriveDocument } from "../../models/googleDriveDocument.model";
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
  openEditor(document: GoogleDriveDocument): void;
  draggable: Draggable;
  lockDropzone: boolean;
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
  openDocument(document?: GoogleDriveDocument): any;
  closeViewFile(): void;
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

      let googleDrivePreference = new Preference();
      let subscription = new Subscription();

      $scope.getGoogleDriveTreeController = function () {
        return angular.element(document.getElementById(googleDriveTree)).scope();
      };

      $scope.isTrashMode = function (): boolean {
        return $scope.getGoogleDriveTreeController()?.isTrashbinOpen ?? false;
      };

      Promise.all([
        initDocumentsContent(googleDriveService, $scope),
        googleDrivePreference.init(),
      ])
        .then(() => {
          $scope.changeViewMode(googleDrivePreference.viewMode);
          $scope.viewList = new GoogleDriveViewList($scope);
          $scope.viewIcons = new GoogleDriveViewIcons($scope);
          $scope.toolbar = new ToolbarSnipletViewModel($scope);
          $scope.isLoaded = true;
          safeApply($scope);
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
              $scope.isLoaded = true;
              safeApply($scope);
            },
          ),
      );

      ($scope as any).$on("$destroy", () => subscription.unsubscribe());

      initDraggable();

      async function initDocumentsContent(
        service: IGoogleDriveService,
        scope: IWorkspaceGoogleDriveContent,
      ): Promise<void> {
        if ($scope.isTrashMode()) return;

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
        const onOsFileDropContent = (e: DragEvent): void => {
          const target = e.target as Element;
          if (!target.closest?.("#google-drive-content")) return;
          if (!Array.from(e.dataTransfer?.types ?? []).includes("Files")) return;
          e.stopPropagation();
          e.preventDefault();
          const files = Array.from(e.dataTransfer?.files ?? []);
          if (files.length === 0) return;
          // Setting lockDropzone=true removes <dropzone-overlay> from the DOM via ng-if.
          // When it re-enters the DOM (lockDropzone=false), the directive re-links and
          // calls scope.hide() so it starts invisible.
          viewModel.lockDropzone = true;
          safeApply($scope);
          const targetFolder = viewModel.parentDocument ?? null;
          const done = (): void => {
            viewModel.lockDropzone = false;
            safeApply($scope);
          };
          googleDriveService
            .uploadLocalFilesToCloud(model.me.userId, files, targetFolder?.id ?? undefined)
            .then(() => {
              googleDriveEventService.sendOpenFolderDocument(
                targetFolder ?? new GoogleDriveDocument().initParent(),
              );
            })
            .catch((err: Error) => {
              console.error("Error uploading local files to Google Drive: " + err.message);
            })
            .then(done, done);
        };
        document.addEventListener("dragover", onOsFileDragOverContent, true);
        document.addEventListener("drop", onOsFileDropContent, true);
        ($scope as any).$on("$destroy", () => {
          document.removeEventListener("dragover", onOsFileDragOverContent, true);
          document.removeEventListener("drop", onOsFileDropContent, true);
        });
      }

      function sortDocumentsByFolder(
        a: GoogleDriveDocument,
        b: GoogleDriveDocument,
      ): number {
        if (a.isFolder && !b.isFolder) return -1;
        if (!a.isFolder && b.isFolder) return 1;
        return 0;
      }

      $scope.moveDocument = async function (
        element: any,
        document: GoogleDriveDocument,
      ): Promise<void> {
        let selectedFolder: GoogleDriveDocument =
          $scope.getGoogleDriveTreeController()?.["selectedFolder"];
        if (!selectedFolder) selectedFolder = $scope.parentDocument;

        const folderContent: any = angular.element(element).scope();
        if (folderContent?.folder instanceof GoogleDriveDocument && folderContent.folder.isFolder) {
          const filesToMove = new Set($scope.selectedDocuments);
          filesToMove.add(document);
          const promises = Array.from(filesToMove)
            .filter((doc) => doc.id !== folderContent.folder.id)
            .map((doc) =>
              googleDriveService.moveDocument(model.me.userId, doc.id, folderContent.folder.id),
            );
          Promise.all(promises)
            .then(() => refreshDocList(selectedFolder))
            .catch((err: AxiosError) => {
              refreshDocList(selectedFolder);
              console.error("Error while moving document: " + err.message);
            });
        }
      };

      function refreshDocList(selectedFolder: GoogleDriveDocument): void {
        $scope.selectedDocuments = [];
        googleDriveService
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
      };

      $scope.openDocument = function (document?: GoogleDriveDocument): any {
        $scope.viewFile = document ?? $scope.selectedDocuments[0];
        template.open("documents-content", `google-drive/content/views/viewer`);
        $scope.selectedDocuments = [];
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

      $scope.openEditor = function (document: GoogleDriveDocument): void {
        googleDriveService.openEditLink(model.me.userId, document);
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
