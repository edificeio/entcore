import { angular, Document, FolderTreeProps, model, ng, template } from "entcore";
import { Tree } from "entcore/types/src/ts/workspace/model";
import { Subscription } from "rxjs";
import { models } from "../../services";
import { EmptyTrashModel } from "./components/folder/emptyTrash.component";
import { FolderCreationModel } from "./components/folder/folderManager.component";
import { DocumentsType } from "./enums/documentsType.enum";
import { Draggable } from "./models/googleDriveDraggable.model";
import { GoogleDriveDocument } from "./models/googleDriveDocument.model";
import { IGoogleDriveEventService } from "./services/googleDriveEvent.service";
import { IGoogleDriveService } from "./services/googleDrive.service";
import { GoogleDriveQuota } from "./models/googleDriveQuota.model";
import { GoogleDriveDocumentsUtils } from "./utils/googleDriveDocuments.utils";
import { safeApply } from "./utils/safeApply.utils";
import { WorkspaceEntcoreUtils } from "./utils/workspaceEntcore.utils";

export interface IGoogleDriveFolderScope {
  documents: Array<GoogleDriveDocument>;
  folderTree: FolderTreeProps;
  selectedFolder: models.Element;
  openedFolder: Array<models.Element>;
  droppable: Draggable;
  dragOverEventListeners: Map<HTMLElement, EventListener>;
  dragLeaveEventListeners: Map<HTMLElement, EventListener>;

  initTree(folder: Array<GoogleDriveDocument>): void;
  watchFolderState(): void;
  openDocument(folder: any): Promise<void>;
  setSwitchDisplayHandler(): void;
  initDraggable(): void;
  resolveDragTarget(event: DragEvent): Promise<void>;
  removeSelectedDocuments(): void;
  addDragEventListeners(): void;
  removeDragEventListeners(): void;
  addDragOverlays(): void;
  removeDragOverlays(): void;
  addDragFeedback(): void;
  removeDragFeedback(): void;
  folderCreation: FolderCreationModel;
  isTrashbinOpen: boolean;
  emptyTrashbin: EmptyTrashModel;
  driveQuota: GoogleDriveQuota;
}

export const workspaceGoogleDriveFolderController = ng.controller(
  "GoogleDriveFolderController",
  [
    "$scope",
    "$rootScope",
    "GoogleDriveService",
    "GoogleDriveEventService",
    (
      $scope: IGoogleDriveFolderScope,
      $rootScope: any,
      googleDriveService: IGoogleDriveService,
      googleDriveEventService: IGoogleDriveEventService,
    ) => {
      $scope.documents = [];
      $scope.folderTree = {};
      $scope.selectedFolder = null;
      $scope.openedFolder = [];
      $scope.dragOverEventListeners = new Map();
      $scope.dragLeaveEventListeners = new Map();
      $scope.folderCreation = new FolderCreationModel($scope);
      $scope.emptyTrashbin = new EmptyTrashModel($scope);
      $scope.isTrashbinOpen = false;
      $rootScope.isGDTrashbinOpen = false;

      const subscriptions: Subscription = new Subscription();

      $scope.initTree = (folder: Array<GoogleDriveDocument>): void => {
        const viewModel: IGoogleDriveFolderScope = $scope;

        const googleDriveElement: HTMLElement = document.querySelector(
          '[application="google-drive"]',
        )?.parentElement;
        if (googleDriveElement) {
          googleDriveElement.parentNode.appendChild(googleDriveElement);
        }

        const staticFolders: Array<GoogleDriveDocument> = [
          GoogleDriveDocument.createStaticFolder("trashbin"),
        ];
        folder.push(...staticFolders);

        $scope.folderTree = {
          cssTree: "folders-tree",
          get trees(): any | Array<Tree> {
            return folder;
          },
          isDisabled(folder: models.Element): boolean {
            return false;
          },
          isOpenedFolder(folder: models.Element): boolean {
            return viewModel.openedFolder.some((f) => f === folder);
          },
          isSelectedFolder(folder: models.Element): boolean {
            return viewModel.selectedFolder === folder;
          },
          async openFolder(folder: models.Element): Promise<void> {
            viewModel.selectedFolder = folder;
            viewModel.setSwitchDisplayHandler();
            viewModel.watchFolderState();

            if (!viewModel.openedFolder.some((f) => f === folder)) {
              viewModel.openedFolder = viewModel.openedFolder.filter(
                (e) => (<any>e).id !== (<any>folder).id,
              );
              viewModel.openedFolder.push(folder);
            }

            await viewModel.openDocument(folder);
            viewModel.removeDragFeedback();
            viewModel.addDragFeedback();
          },
        };
      };

      // Walk up from a DOM element to find an Angular scope with a GD folder.
      // Used by both onOsFileDrop and resolveDragTarget.
      function findFolderScope(target: Element): any {
        let el: Element | null = target;
        const treeEl = document.getElementById("google-drive-folder-tree");
        while (el && el !== treeEl) {
          const s = angular.element(el).scope();
          if (s?.folder instanceof GoogleDriveDocument) return s;
          el = el.parentElement as (Element | null);
        }
        return null;
      }

      // Walk up from a DOM element to find an ENT workspace folder (models.Element with _id,
      // not a GoogleDriveDocument). Called during GD→workspace drops.
      function findEntwsFolder(target: Element): any | null {
        let el: Element | null = target;
        let depth = 0;
        while (el && el !== document.body && depth < 20) {
          const s: any = angular.element(el).scope();
          const f = s?.folder;
          // Accept ENT workspace folders: not a GoogleDriveDocument, and has _id (user-created folder)
          // OR has name (virtual root like "Documents personnels" with no _id — copies to workspace root).
          if (f != null && !(f instanceof GoogleDriveDocument) && (f._id || f.name !== undefined)) {
            return f;
          }
          el = el.parentElement as Element | null;
          depth++;
        }
        return null;
      }

      $scope.initDraggable = (): void => {
        const viewModel: IGoogleDriveFolderScope = $scope;
        $scope.droppable = {
          dragConditionHandler(event: DragEvent, content?: any): boolean {
            return false;
          },
          async dragDropHandler(event: DragEvent): Promise<void> {
            await viewModel.resolveDragTarget(event);
          },
          dragEndHandler(event: DragEvent, content?: any): void {},
          dragStartHandler(event: DragEvent, content?: any): void {},
          dropConditionHandler(event: DragEvent, content?: any): boolean {
            return false;
          },
        };

        // Capture-phase drop listener: fires before the entcore dragdrop jQuery handler,
        // which crashes on OS file drops (JSON.parse("") fails).
        // For OS drops on the GD folder tree: stop propagation to prevent the crash,
        // then upload the files to ENT workspace and move them to GD.
        const onOsFileDrop = (e: DragEvent): void => {
          const target = e.target as Element;
          if (!target.closest?.("#google-drive-folder-tree")) return;
          if (!Array.from(e.dataTransfer?.types ?? []).includes("Files")) return;
          e.stopPropagation();
          e.preventDefault();

          const files = Array.from(e.dataTransfer?.files ?? []);
          if (files.length === 0) return;

          const folderScopeEl = findFolderScope(target);
          const targetFolder: GoogleDriveDocument | null =
            folderScopeEl?.folder instanceof GoogleDriveDocument
              ? (folderScopeEl.folder as GoogleDriveDocument)
              : null;

          googleDriveService
            .uploadLocalFilesToCloud(model.me.userId, files, targetFolder?.id ?? undefined)
            .then(() => {
              googleDriveEventService.sendOpenFolderDocument(
                targetFolder ?? new GoogleDriveDocument().initParent(),
              );
            })
            .catch((err: Error) => {
              console.error("Error uploading local files to Google Drive: " + err.message);
            });
        };
        // Capture-phase dragover: guarantee preventDefault() for OS drags on the GD folder tree.
        // Without this, the browser may not fire the drop event at all if no lower handler
        // has called preventDefault() yet.
        const onOsFileDragOver = (e: DragEvent): void => {
          const target = e.target as Element;
          if (!target.closest?.("#google-drive-folder-tree")) return;
          if (!Array.from(e.dataTransfer?.types ?? []).includes("Files")) return;
          e.preventDefault();
          if (e.dataTransfer) e.dataTransfer.dropEffect = "copy";
        };

        document.addEventListener("dragover", onOsFileDragOver, true);
        document.addEventListener("drop", onOsFileDrop, true);
        ($scope as any).$on("$destroy", () => {
          document.removeEventListener("dragover", onOsFileDragOver, true);
          document.removeEventListener("drop", onOsFileDrop, true);
        });

        // GD document → ENT workspace folder: allow the drop (preventDefault) and copy
        // the GD document(s) to the target ENT workspace folder.
        // Uses capture phase so this fires before the workspace dragdrop directive (bubble),
        // which would otherwise reject the drop (draggingItems is empty during a GD drag).
        const onGdToWsDragOver = (e: DragEvent): void => {
          const contentContext = googleDriveEventService.getContentContext();
          if (!contentContext) return;
          const target = e.target as Element;
          if (target.closest?.("#google-drive-folder-tree")) return;
          if (target.closest?.("#google-drive-content")) return;
          // .folder-tree is the class on ENT workspace folder tree container divs.
          // highlight-title spans are siblings of <a dragdrop="...">, not children,
          // so closest('[dragdrop]') fails when hovering over them.
          if (!target.closest?.(".folder-tree")) return;
          if (!findEntwsFolder(target)) return;
          e.preventDefault();
          if (e.dataTransfer) e.dataTransfer.dropEffect = "copy";
        };

        const onGdToWsDrop = (e: DragEvent): void => {
          const contentContext = googleDriveEventService.getContentContext();
          if (!contentContext) return;
          const target = e.target as Element;
          if (target.closest?.("#google-drive-folder-tree")) return;
          if (target.closest?.("#google-drive-content")) return;
          if (!target.closest?.(".folder-tree")) return;
          const wsFolder = findEntwsFolder(target);
          if (!wsFolder) return;
          e.stopPropagation();
          e.preventDefault();
          document.querySelectorAll(".droptarget").forEach((el) => el.classList.remove("droptarget"));
          const contentEl = document.getElementById("google-drive-content");
          const contentScope: any = contentEl ? angular.element(contentEl).scope() : null;
          const selectedDocs: GoogleDriveDocument[] = contentScope?.selectedDocuments ?? [];
          const idsToCopy: string[] = selectedDocs
            .filter((d: GoogleDriveDocument) => !!d.id)
            .map((d: GoogleDriveDocument) => d.id);
          if (contentContext.id && !idsToCopy.includes(contentContext.id)) {
            idsToCopy.push(contentContext.id);
          }
          // wsFolder._id is undefined for virtual root folders (e.g. "Documents personnels").
          // Passing undefined to copyDocumentToWorkspace omits the parentId param → copies to workspace root.
          googleDriveService
            .copyDocumentToWorkspace(model.me.userId, idsToCopy, wsFolder._id || undefined)
            .then(() => {
              googleDriveEventService.setContentContext(null);
              WorkspaceEntcoreUtils.updateWorkspaceDocuments(wsFolder);
            })
            .catch((err: Error) => {
              console.error("[GD→WS] copy failed:", err.message);
              googleDriveEventService.setContentContext(null);
            });
        };

        document.addEventListener("dragover", onGdToWsDragOver, true);
        document.addEventListener("drop", onGdToWsDrop, true);

        // ENT workspace → GD folder tree: capture-phase so we bypass the entcore
        // dragdrop directive on <folder-tree> which evaluates in an isolated scope
        // where $scope.droppable is undefined and silently fails.
        const onWsToGdDragOver = (e: DragEvent): void => {
          const target = e.target as Element;
          if (!target.closest?.("#google-drive-folder-tree")) return;
          // If contentContext is set, this is a GD→GD drag — handled elsewhere.
          if (googleDriveEventService.getContentContext()) return;
          const types = Array.from(e.dataTransfer?.types ?? []);
          if (!types.includes("application/json")) return;
          // Accept the drop for any area of the GD folder tree. findFolderScope is only used
          // in the drop handler (where it can walk up from the precise element under the cursor).
          // Checking it here would show "forbidden" over container elements (nav, isolated scope
          // wrapper) where the GD folder scope is on descendant elements, not ancestors.
          e.preventDefault();
          if (e.dataTransfer) e.dataTransfer.dropEffect = "move";
        };

        const onWsToGdDrop = (e: DragEvent): void => {
          const target = e.target as Element;
          if (!target.closest?.("#google-drive-folder-tree")) return;
          if (googleDriveEventService.getContentContext()) return;

          let docData: any = null;
          try {
            docData = JSON.parse(e.dataTransfer?.getData("application/json") ?? "null");
          } catch (_) {
            return;
          }
          if (!docData || !docData._id) return;

          // Try to find the specific GD folder under the cursor. If the drop landed on a
          // container element (nav, isolated-scope wrapper), fall back to the currently
          // selected GD folder, then to GD root.
          const folderScope = findFolderScope(target);
          const targetFolder: GoogleDriveDocument =
            folderScope?.folder instanceof GoogleDriveDocument
              ? folderScope.folder
              : $scope.selectedFolder instanceof GoogleDriveDocument
                ? $scope.selectedFolder
                : new GoogleDriveDocument().initParent();

          e.stopPropagation();
          e.preventDefault();
          document.querySelectorAll(".droptarget").forEach((el) => el.classList.remove("droptarget"));
          // jQuery/CSS cleanup (position:absolute, mousemove.drag, dragImageCopy) is handled by the
          // capture-phase dragend listener registered in $scope.drag (drag.ts). That listener fires
          // on dragend which follows drop, so no explicit cleanup is needed here.

          let selectedDocuments: Array<Document> =
            WorkspaceEntcoreUtils.workspaceScope()["documentList"]["_documents"];
          selectedDocuments = selectedDocuments.concat(
            WorkspaceEntcoreUtils.workspaceScope()["currentTree"]["children"],
          );
          const docIds: string[] = selectedDocuments
            .filter((file: Document) => file.selected && file._id)
            .map((file: Document) => file._id);
          if (!docIds.includes(docData._id)) docIds.push(docData._id);

          googleDriveService
            .moveDocumentWorkspaceToCloud(model.me.userId, docIds, targetFolder.id)
            .then(() => {
              const wsScope: any = WorkspaceEntcoreUtils.workspaceScope();
              WorkspaceEntcoreUtils.updateWorkspaceDocuments(wsScope["openedFolder"]["folder"]);
              if (typeof wsScope?.reloadFolderContent === "function") {
                wsScope.reloadFolderContent();
              }
              googleDriveEventService.sendOpenFolderDocument(targetFolder);
              $scope.selectedFolder = null;
            })
            .catch((err: Error) => {
              console.error("[WS→GD] move failed:", err.message);
            });
        };

        document.addEventListener("dragover", onWsToGdDragOver, true);
        document.addEventListener("drop", onWsToGdDrop, true);
        ($scope as any).$on("$destroy", () => {
          document.removeEventListener("dragover", onGdToWsDragOver, true);
          document.removeEventListener("drop", onGdToWsDrop, true);
          document.removeEventListener("dragover", onWsToGdDragOver, true);
          document.removeEventListener("drop", onWsToGdDrop, true);
        });
      };

      function removeDropTarget(event: DragEvent): void {
        const target = event.target as HTMLElement;
        const droppable = target.closest(".folder-list-item") || target;
        if (droppable) droppable.classList.remove("droptarget");
      }

      $scope.resolveDragTarget = async (event: DragEvent): Promise<void> => {
        removeDropTarget(event);

        const contentContext = googleDriveEventService.getContentContext();
        if (contentContext) {
          // GD→GD drag: move GD document(s) to the target GD folder
          const folderScope = findFolderScope(event.target as Element);
          if (folderScope?.folder instanceof GoogleDriveDocument && folderScope.folder.isFolder) {
            const targetFolder: GoogleDriveDocument = folderScope.folder;

            const contentEl = document.getElementById("google-drive-content");
            const contentScope: any = contentEl ? angular.element(contentEl).scope() : null;
            const selectedDocs: GoogleDriveDocument[] = contentScope?.selectedDocuments ?? [];

            const filesToMove = new Set<GoogleDriveDocument>(selectedDocs);
            filesToMove.add(contentContext);

            const parentDocument: GoogleDriveDocument = contentScope?.parentDocument ?? null;

            const isTrashTarget = targetFolder.isStaticFolder && targetFolder.staticFolderType === "trashbin";
            const promises = Array.from(filesToMove)
              .filter((doc) => doc.id !== targetFolder.id)
              .map((doc) =>
                isTrashTarget
                  ? googleDriveService.deleteDocuments(model.me.userId, [doc.id])
                  : googleDriveService.moveDocument(model.me.userId, doc.id, targetFolder.id),
              );

            Promise.all(promises)
              .then(() => {
                googleDriveEventService.setContentContext(null);
                googleDriveEventService.sendOpenFolderDocument(
                  parentDocument ?? new GoogleDriveDocument().initParent(),
                );
                $scope.selectedFolder = null;
              })
              .catch((err: Error) => {
                console.error("Error while moving GD document: " + err.message);
                googleDriveEventService.setContentContext(null);
              });
          }
          return;
        }

        // workspace→GD drag: any ENT workspace item has _id; GD items have id not _id.
        // Do not check eType — workspace files use eType="media", not "file".
        let docData: any = null;
        try {
          docData = JSON.parse(event.dataTransfer.getData("application/json"));
        } catch (_) {
          return;
        }
        if (docData && docData._id) {
          const folderScope = findFolderScope(event.target as Element);
          if (folderScope?.folder instanceof GoogleDriveDocument) {
            const targetFolder: GoogleDriveDocument = folderScope.folder;
            let selectedDocuments: Array<Document> =
              WorkspaceEntcoreUtils.workspaceScope()["documentList"]["_documents"];
            selectedDocuments = selectedDocuments.concat(
              WorkspaceEntcoreUtils.workspaceScope()["currentTree"]["children"],
            );
            const docIds: Set<string> = new Set(
              selectedDocuments
                .filter((file: Document) => file.selected)
                .map((file: Document) => file._id),
            );
            docIds.add(docData._id);

            googleDriveService
              .moveDocumentWorkspaceToCloud(
                model.me.userId,
                Array.from(docIds),
                targetFolder.id,
              )
              .then(() => {
                WorkspaceEntcoreUtils.updateWorkspaceDocuments(
                  WorkspaceEntcoreUtils.workspaceScope()["openedFolder"]["folder"],
                );
                googleDriveEventService.sendOpenFolderDocument(
                  folderScope.folder,
                );
                $scope.selectedFolder = null;
              })
              .catch((err: Error) => {
                console.error("Error while moving workspace document to Google Drive: " + err.message);
              });
          }
        }
      };

      $scope.watchFolderState = (): void => {
        const folderArrows = document.querySelectorAll(
          "#google-drive-folder-tree i",
        );
        folderArrows.forEach((element) => {
          element.removeEventListener("click", onClickFolder($scope));
        });
        const viewModel: IGoogleDriveFolderScope = $scope;
        folderArrows.forEach((element) => {
          element.addEventListener("click", onClickFolder(viewModel));
        });
      };

      $scope.openDocument = async (document: any): Promise<void> => {
        if (document.isStaticFolder) {
          const staticType: string = document.staticFolderType;
          let staticDocuments: Array<GoogleDriveDocument> = [];

          switch (staticType) {
            case "trashbin":
              $scope.isTrashbinOpen = true;
              $rootScope.isGDTrashbinOpen = true;
              template.close('lightbox');
              safeApply($scope);
              const trashList = await googleDriveService
                .listTrash(model.me.userId)
                .catch((err: Error) => {
                  console.error("Error fetching trash: " + err.message);
                  return [];
                });
              staticDocuments = trashList;
          }

          $scope.documents = staticDocuments;
          googleDriveEventService.sendDocuments({
            parentDocument: document,
            documents: staticDocuments,
          });
          safeApply($scope);
          return;
        }

        $scope.isTrashbinOpen = false;
        $rootScope.isGDTrashbinOpen = true;
        template.close('lightbox');
        safeApply($scope);

        let docs: Array<GoogleDriveDocument> = await googleDriveService
          .listDocument(model.me.userId, document.id || null)
          .catch((err: Error) => {
            console.error("Error fetching folder documents: " + err.message);
            return [];
          });

        document.children = docs
          .filter(GoogleDriveDocumentsUtils.filterRemoveOwnDocument(document))
          .filter(GoogleDriveDocumentsUtils.filterDocumentOnly());

        safeApply($scope);
        googleDriveEventService.sendDocuments({
          parentDocument: document.id ? document : new GoogleDriveDocument().initParent(),
          documents: docs.filter(GoogleDriveDocumentsUtils.filterRemoveOwnDocument(document)),
        });
      };

      $scope.setSwitchDisplayHandler = (): void => {
        const viewModel: IGoogleDriveFolderScope = $scope;

        const googleDriveFolder = document.querySelector("#google-drive-folder-tree");
        if (googleDriveFolder) {
          const oldHandler = googleDriveFolder["workspaceGoogleDriveHandler"];
          if (oldHandler) googleDriveFolder.removeEventListener("click", oldHandler);
          const newHandler = switchWorkspaceTreeHandler();
          googleDriveFolder["workspaceGoogleDriveHandler"] = newHandler;
          googleDriveFolder.addEventListener("click", newHandler);
        }

        const workspaceTree = document.querySelector(WorkspaceEntcoreUtils.$ENTCORE_WORKSPACE);
        if (workspaceTree) {
          const oldHandler = workspaceTree["googleDriveHandler"];
          if (oldHandler) workspaceTree.removeEventListener("click", oldHandler);
          const newHandler = switchGoogleDriveTreeHandler(viewModel);
          workspaceTree["googleDriveHandler"] = newHandler;
          workspaceTree.addEventListener("click", newHandler);
        }
      };

      $scope.removeSelectedDocuments = (): void => {
        const selectedDocuments: Array<Document> =
          WorkspaceEntcoreUtils.workspaceScope()["openedFolder"]["documents"];
        const folders: Array<Document> =
          WorkspaceEntcoreUtils.workspaceScope()["openedFolder"]["folders"];
        if (selectedDocuments && folders) {
          selectedDocuments.forEach((doc) => (doc.selected = false));
          folders.forEach((fol) => (fol.selected = false));
        }
      };

      $scope.addDragEventListeners = (): void => {
        const folders: HTMLElement[] = Array.from(
          document.getElementsByTagName("folder-tree-inner"),
        ) as HTMLElement[];
        folders.forEach((element) => {
          element.addEventListener("dragover", onDragOver(element));
          element.addEventListener("dragleave", onDragLeave(element));
          $scope.dragOverEventListeners.set(element, onDragOver(element));
          $scope.dragLeaveEventListeners.set(element, onDragLeave(element));
        });
      };

      $scope.addDragOverlays = (): void => {
        const folders: HTMLElement[] = Array.from(
          document.getElementsByTagName("folder-tree-inner"),
        ) as HTMLElement[];
        folders.forEach((element, i) => {
          const span = document.createElement("span");
          span.id = "droptarget-gd-" + i;
          span.className = "highlight-title highlight-title-border ng-scope";
          const subSpan = document.createElement("span");
          subSpan.className = "count-badge ng-binding";
          span.appendChild(subSpan);
          const ul = element.lastElementChild;
          if (ul?.tagName === "UL") {
            element.insertBefore(span, ul);
          } else {
            element.appendChild(span);
          }
          element.style.position = "relative";
          element.style.display = "block";
        });
      };

      $scope.removeDragOverlays = (): void => {
        const spans = Array.from(
          document.querySelectorAll(`[id^="droptarget-gd-"]`),
        ) as HTMLElement[];
        spans.forEach((element) => element.remove());
      };

      $scope.removeDragEventListeners = (): void => {
        $scope.dragOverEventListeners.forEach((listener, element) =>
          element.removeEventListener("dragover", onDragOver(element)),
        );
        $scope.dragOverEventListeners.clear();
        $scope.dragLeaveEventListeners.forEach((listener, element) =>
          element.removeEventListener("dragleave", onDragLeave(element)),
        );
        $scope.dragLeaveEventListeners.clear();
      };

      $scope.addDragFeedback = (): void => {
        $scope.addDragOverlays();
        $scope.addDragEventListeners();
      };

      $scope.removeDragFeedback = (): void => {
        $scope.removeDragOverlays();
        $scope.removeDragEventListeners();
        document.querySelectorAll(".droptarget").forEach((el) => el.classList.remove("droptarget"));
      };

      // Initialize the root folder tree
      $scope.documents = [new GoogleDriveDocument().initParent()];
      $scope.initTree($scope.documents);
      $scope.initDraggable();

      function refreshQuota(): void {
        googleDriveService
          .getStorageQuota(model.me.userId)
          .then((quota: GoogleDriveQuota) => {
            $scope.driveQuota = quota;
            safeApply($scope);
          })
          .catch(() => {
            $scope.driveQuota = null;
          });
      }

      refreshQuota();

      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.get("folder") === "google-drive") {
        $scope.openDocument($scope.documents[0]).then(() => {
          $scope.folderTree.openFolder($scope.documents[0]);
          $scope.setSwitchDisplayHandler();
          template.open("documents", "google-drive/content/workspace-google-drive-content");
          WorkspaceEntcoreUtils.toggleWorkspaceContentDisplay(false);
          safeApply($scope);
        });
      }

      subscriptions.add(
        googleDriveEventService
          .getOpenedFolderDocument()
          .subscribe((document: GoogleDriveDocument) => {
            let getFolderContext: GoogleDriveDocument = $scope.folderTree.trees.find(
              (f: any) => f.id === document.id,
            );
            $scope.folderTree.openFolder(
              getFolderContext ? getFolderContext : document,
            );
            refreshQuota();
          }),
      );

      subscriptions.add(
        googleDriveEventService.getQuotaRefresh().subscribe(() => refreshQuota()),
      );

      function onDragLeave(element: HTMLElement): EventListener {
        return function (event: Event): void {
          event.preventDefault();
          event.stopPropagation();
          element.firstElementChild.classList.remove("droptarget");
        };
      }

      function onDragOver(element: HTMLElement): EventListener {
        return function (event: Event): void {
          event.preventDefault();
          event.stopPropagation();
          element.firstElementChild.classList.add("droptarget");
        };
      }

      function switchWorkspaceTreeHandler() {
        const viewModel: IGoogleDriveFolderScope = $scope;
        return function (): void {
          if (!viewModel.selectedFolder) {
            viewModel.folderTree.openFolder(viewModel.documents[0]);
          }
          const workspaceFolderTree = document.querySelectorAll(
            WorkspaceEntcoreUtils.$ENTCORE_WORKSPACE + " li a",
          );
          template.open("documents", "google-drive/content/workspace-google-drive-content");
          viewModel.removeSelectedDocuments();
          workspaceFolderTree.forEach((element) => element.classList.remove("selected"));
          WorkspaceEntcoreUtils.toggleWorkspaceContentDisplay(false);
        };
      }

      function switchGoogleDriveTreeHandler(viewModel: IGoogleDriveFolderScope) {
        return function (): void {
          let element: Element = arguments[0].target;
          let target: Element;
          if (element?.tagName === "A") {
            target = element;
          } else if (element?.parentElement?.tagName === "A") {
            target = element.parentElement;
          }

          if (target && viewModel.selectedFolder) {
            viewModel.selectedFolder = null;
            $rootScope.isGDTrashbinOpen = false;
            target.classList.add("selected");
            WorkspaceEntcoreUtils.updateWorkspaceDocuments(
              angular.element(target).scope().folder,
            );
            WorkspaceEntcoreUtils.workspaceScope()["openedFolder"]["folder"] =
              angular.element(target).scope().folder;
            WorkspaceEntcoreUtils.toggleWorkspaceContentDisplay(true);
            googleDriveEventService.setContentContext(null);
            template.open("documents", "icons");
          }
        };
      }

      function onClickFolder(viewModel: IGoogleDriveFolderScope) {
        return function () {
          event.stopPropagation();
          const scope: any = angular.element(arguments[0].target).scope();
          const folder: models.Element = scope.folder;
          if (viewModel.openedFolder.some((f) => f === folder)) {
            viewModel.openedFolder = viewModel.openedFolder.filter((f) => f !== folder);
          } else {
            viewModel.openedFolder.push(folder);
          }
          safeApply(scope);
        };
      }
    },
  ],
);

export const workspaceGoogleDriveFolder = ng.directive(
  "workspaceGoogleDriveFolder",
  () => {
    return {
      restrict: "E",
      templateUrl:
        "/workspace/public/template/google-drive/folder/workspace-google-drive-folder.html",
      controller: "GoogleDriveFolderController",
    };
  },
);
