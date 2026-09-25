import { angular, Document, FolderTreeProps, idiom as lang, model, ng, template } from "entcore";
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
import { nextcloudEventService } from "../nextcloud/services/nextcloudEvent.service";

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
  isSharedViewOpen: boolean;
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
      $scope.isSharedViewOpen = false;
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
          GoogleDriveDocument.createStaticFolder("shared"),
          GoogleDriveDocument.createStaticFolder("trashbin"),
        ];
        folder.push(...staticFolders);

        // folder[0] (from initParent()) now nests under the "Google Drive" grouping node as "Mon Drive".
        if (folder[0]) {
          folder[0].name = lang.translate("google-drive.mydrive");
        }
        const rootGroup: GoogleDriveDocument = GoogleDriveDocument.createRootGroup();
        rootGroup.children = folder;
        viewModel.openedFolder = [rootGroup as any];

        $scope.folderTree = {
          cssTree: "folders-tree",
          get trees(): any | Array<Tree> {
            return [rootGroup];
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
            // Clicking the "Google Drive" grouping label toggles fold/unfold and also opens "Mon Drive".
            if ((folder as any).isRootGroup) {
              if (viewModel.openedFolder.some((f) => f === folder)) {
                viewModel.openedFolder = viewModel.openedFolder.filter((f) => f !== folder);
              } else {
                viewModel.openedFolder.push(folder);
              }
              setTimeout(injectRootGroupIcon, 0);
              return $scope.folderTree.openFolder(rootGroup.children[0] as any);
            }

            viewModel.selectedFolder = folder;
            // Nextcloud's tree has its own independent selectedFolder; clear it or its last-selected row stays highlighted.
            const ncTreeEl = document.getElementById("nextcloud-folder-tree");
            const ncTreeScope: any = angular.element(ncTreeEl).scope();
            if (ncTreeScope) {
              ncTreeScope.selectedFolder = null;
              const phase = ncTreeScope.$root && ncTreeScope.$root.$$phase;
              if (!phase) {
                ncTreeScope.$apply();
              }
            }
            // Only clear "selected" (imperative DOM class); clearing "opened" would collapse the tree.
            if (ncTreeEl) {
              ncTreeEl.querySelectorAll("a.selected").forEach((el) => {
                el.classList.remove("selected");
              });
            }
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
            setTimeout(injectRootGroupIcon, 0);
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
          // A Nextcloud-sourced drag also carries "application/json" — no transfer path exists
          // between Nextcloud and Google Drive, so leave the browser's default "no-drop" cursor.
          if (nextcloudEventService.getContentContext()) return;
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
          const isSharedTarget: boolean =
            folderScope?.folder instanceof GoogleDriveDocument &&
            folderScope.folder.isStaticFolder &&
            folderScope.folder.staticFolderType === "shared";
          if (
            folderScope?.folder instanceof GoogleDriveDocument &&
            folderScope.folder.isFolder &&
            !isSharedTarget
          ) {
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
        if (document.isRootGroup) {
          // Grouping node — children are set once in initTree() and must not be overwritten here.
          return;
        }
        if (document.isStaticFolder) {
          const staticType: string = document.staticFolderType;
          let staticDocuments: Array<GoogleDriveDocument> = [];

          switch (staticType) {
            case "trashbin":
              $scope.isTrashbinOpen = true;
              $scope.isSharedViewOpen = false;
              $rootScope.isGDTrashbinOpen = true;
              $rootScope.isNextcloudTrashbinOpen = false;
              template.close('lightbox');
              safeApply($scope);
              const trashList = await googleDriveService
                .listTrash(model.me.userId)
                .catch((err: Error) => {
                  console.error("Error fetching trash: " + err.message);
                  return [];
                });
              staticDocuments = trashList;
              break;
            case "shared":
              $scope.isTrashbinOpen = false;
              $scope.isSharedViewOpen = true;
              $rootScope.isGDTrashbinOpen = true;
              $rootScope.isNextcloudTrashbinOpen = false;
              template.close('lightbox');
              safeApply($scope);
              const sharedList = await googleDriveService
                .listSharedFiles(model.me.userId)
                .catch((err: Error) => {
                  console.error("Error fetching shared files: " + err.message);
                  return [];
                });
              staticDocuments = sharedList;
              break;
          }

          // Don't assign to $scope.documents: documents[0] is relied on elsewhere (e.g. computeBreadcrumb).
          googleDriveEventService.sendDocuments({
            parentDocument: document,
            documents: staticDocuments,
          });
          safeApply($scope);
          return;
        }

        $scope.isTrashbinOpen = false;
        $scope.isSharedViewOpen = false;
        $rootScope.isGDTrashbinOpen = true;
        $rootScope.isNextcloudTrashbinOpen = false;
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
      setTimeout(injectRootGroupIcon, 0);
      // Attach upfront, otherwise the first tree click fires before openFolder() would attach it.
      $scope.setSwitchDisplayHandler();

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

      // <folder-tree-inner> has no icon slot, so icons are injected as DOM nodes; currentColor lets mobile nav CSS flip them to white.
      const FOLDER_ICON_SVG: string =
        '<path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>';
      const SHARE_ICON_SVG: string =
        '<path fill="currentColor" d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"/>';
      const TRASH_ICON_SVG: string =
        '<path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6zM10 11v6M14 11v6"/>';
      const FOLDER_OPEN_ICON_SVG: string =
        '<path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M2 8V6a2 2 0 0 1 2-2h4.5l2 2H20a2 2 0 0 1 2 2"/>' +
        '<path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M2 8h19a1 1 0 0 1 .97 1.24l-1.5 6A2 2 0 0 1 18.53 17H4.5a2 2 0 0 1-1.94-1.51L1 9.5A1 1 0 0 1 2 8Z"/>';

      const GOOGLE_DRIVE_ROOT_SVG: string =
        '<path fill="#0066da" d="m6.6 66.85 3.85 6.65c.8 1.4 1.95 2.5 3.3 3.3l13.75-23.8h-27.5c0 1.55.4 3.1 1.2 4.5z"/>' +
        '<path fill="#00ac47" d="m43.65 25-13.75-23.8c-1.35.8-2.5 1.9-3.3 3.3l-25.4 44a9.06 9.06 0 0 0 -1.2 4.5h27.5z"/>' +
        '<path fill="#ea4335" d="m73.55 76.8c1.35-.8 2.5-1.9 3.3-3.3l1.6-2.75 7.65-13.25c.8-1.4 1.2-2.95 1.2-4.5h-27.5l5.85 11.5z"/>' +
        '<path fill="#00832d" d="m43.65 25 13.75-23.8c-1.35-.8-2.9-1.2-4.5-1.2h-18.5c-1.6 0-3.15.45-4.5 1.2z"/>' +
        '<path fill="#2684fc" d="m59.8 53h-32.3l-13.75 23.8c1.35.8 2.9 1.2 4.5 1.2h50.8c1.6 0 3.15-.45 4.5-1.2z"/>' +
        '<path fill="#ffba00" d="m73.4 26.5-12.7-22c-.8-1.4-1.95-2.5-3.3-3.3l-13.75 23.8 16.15 28h27.45c0-1.55-.4-3.1-1.2-4.5z"/>';

      function prependIcon(
        link: Element,
        innerSvg: string,
        markerClass: string,
        viewBox: string = "0 0 24 24",
        replaceMarkerClasses: Array<string> = [],
      ): void {
        if (!link) return;
        // Must match !important to beat the theme's own "white-space:normal !important" rule.
        (link as HTMLElement).style.setProperty("white-space", "nowrap", "important");
        // Icons (e.g. open/closed folder) can swap on state change — drop the stale variant first.
        replaceMarkerClasses.forEach((cls) => {
          if (cls === markerClass) return;
          const stale = link.querySelector("." + cls);
          if (stale) stale.remove();
        });
        if (link.querySelector("." + markerClass)) return;
        const icon = document.createElement("span");
        icon.className = markerClass;
        icon.style.cssText =
          "display:inline-block;width:16px;height:16px;margin-right:6px;vertical-align:middle;flex-shrink:0;color:#8c939e;";
        icon.innerHTML =
          '<svg xmlns="http://www.w3.org/2000/svg" viewBox="' +
          viewBox +
          '" width="16" height="16">' +
          innerSvg +
          "</svg>";
        link.prepend(icon);
      }

      // The row shares one ng-click across arrow and label, so the arrow needs its own toggle to collapse independently.
      function bindFolderToggle(link: Element, folder: GoogleDriveDocument): void {
        // Excludes the fake arrow (ensureExpandArrow below), which has its own click handler.
        const arrowEl = link.querySelector("i.arrow:not(.google-drive-fake-arrow)") as
          | (HTMLElement & { __gdToggleBound?: boolean })
          | null;
        if (!arrowEl || arrowEl.__gdToggleBound) return;
        arrowEl.__gdToggleBound = true;
        arrowEl.addEventListener("click", (event: Event) => {
          event.stopPropagation();
          event.preventDefault();
          const index = $scope.openedFolder.indexOf(folder as any);
          if (index > -1) {
            $scope.openedFolder.splice(index, 1);
          } else {
            $scope.openedFolder.push(folder as any);
          }
          safeApply($scope);
          setTimeout(injectRootGroupIcon, 0);
        });
      }

      // children is [] until a folder is navigated into, so a real arrow won't show yet; inject a fake one that opens the folder on click.
      function ensureExpandArrow(link: Element, folder: GoogleDriveDocument): void {
        const fakeArrow = link.querySelector(".google-drive-fake-arrow");
        const hasRealArrow = !!link.querySelector("i.arrow:not(.google-drive-fake-arrow)");
        const notYetChecked = !folder.children || folder.children.length === 0;

        if (hasRealArrow || !notYetChecked) {
          fakeArrow?.remove();
          return;
        }
        if (fakeArrow) return;

        const arrow = document.createElement("i");
        arrow.className = "arrow google-drive-fake-arrow";
        arrow.addEventListener("click", (event: Event) => {
          event.stopPropagation();
          event.preventDefault();
          $scope.folderTree.openFolder(folder as any);
        });
        link.prepend(arrow);
      }

      function injectRootGroupIcon(): void {
        const treeEl = document.getElementById("google-drive-folder-tree");
        if (!treeEl) return;

        const links = treeEl.querySelectorAll("folder-tree-inner > a.folder-list-item");
        links.forEach((link) => {
          const innerEl: any = link.closest("folder-tree-inner");
          if (!innerEl) return;
          const innerJq: any = angular.element(innerEl);
          // .folder lives on the isolate scope, not the surrounding ng-repeat scope .scope() returns.
          const scope: any = innerJq.isolateScope?.() ?? innerJq.scope?.();
          const folder: GoogleDriveDocument = scope?.folder;
          if (!folder) return;

          if (folder.isRootGroup) {
            prependIcon(link, GOOGLE_DRIVE_ROOT_SVG, "google-drive-root-icon", "0 0 87.3 78");
          } else if (folder.staticFolderType === "shared") {
            prependIcon(link, SHARE_ICON_SVG, "google-drive-child-icon");
          } else if (folder.staticFolderType === "trashbin") {
            prependIcon(link, TRASH_ICON_SVG, "google-drive-child-icon");
          } else {
            bindFolderToggle(link, folder);
            const isOpen = $scope.openedFolder?.some((f: any) => f === folder) ?? false;
            if (isOpen) {
              prependIcon(link, FOLDER_OPEN_ICON_SVG, "google-drive-child-icon-open", "0 0 24 24", [
                "google-drive-child-icon",
                "google-drive-child-icon-open",
              ]);
            } else {
              prependIcon(link, FOLDER_ICON_SVG, "google-drive-child-icon", "0 0 24 24", [
                "google-drive-child-icon",
                "google-drive-child-icon-open",
              ]);
            }
            ensureExpandArrow(link, folder);
          }
        });
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
            const classicScope: any = WorkspaceEntcoreUtils.workspaceScope();
            let folder: any = angular.element(target).scope().folder;
            // Guard against the "Mon espace personnel" grouping header itself landing in openedFolder.folder.
            const wrapperRoot = classicScope?.wrapperTrees?.[0];
            const redirectedFromWrapper = folder === wrapperRoot;
            if (redirectedFromWrapper) {
              folder = (classicScope?.trees || []).find((t: any) => t.filter === "owner") ?? folder;
            }
            viewModel.selectedFolder = null;
            $rootScope.isGDTrashbinOpen = false;
            if (!redirectedFromWrapper) {
              target.classList.add("selected");
            }
            if (redirectedFromWrapper) {
              // setCurrentTree actually fetches content; the plain assignment below only swaps the reference.
              classicScope.setCurrentTree?.("owner");
            } else {
              WorkspaceEntcoreUtils.updateWorkspaceDocuments(folder);
              classicScope["openedFolder"]["folder"] = folder;
            }
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
          setTimeout(injectRootGroupIcon, 0);
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
