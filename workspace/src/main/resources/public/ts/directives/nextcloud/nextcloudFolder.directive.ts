import { angular, Document, FolderTreeProps, model, ng, template, } from "entcore";
import { Tree } from "entcore/types/src/ts/workspace/model";
import { Subscription } from "rxjs";
import { models } from "../../services";
import { EmptyTrashModel } from "./components/folder/emptyTrash.component";
import { FolderCreationModel } from "./components/folder/folderManager.component";
import { DocumentsType } from "./enums/documentsType.enum";
import { Draggable } from "./models/nextcloudDraggable.model";
import { SyncDocument } from "./models/nextcloudFolder.model";
import { UserNextcloud } from "./models/nextcloudUser.model";
import { INextcloudEventService } from "./services/nextcloudEvent.service";
import { INextcloudUserService } from "./services/nextcloudUser.service";
import { INextcloudService } from "./services/nextcloud.service";
import { NextcloudDocumentsUtils } from "./utils/nextcloudDocuments.utils";
import { safeApply } from "./utils/safeApply.utils";
import { WorkspaceEntcoreUtils } from "./utils/workspaceEntcore.utils";
import { googleDriveEventService } from "../google-drive/services/googleDriveEvent.service";

export interface INextcloudFolderScope {
  documents: Array<SyncDocument>;
  userInfo: UserNextcloud;
  folderTree: FolderTreeProps;
  selectedFolder: models.Element;
  openedFolder: Array<models.Element>;
  droppable: Draggable;
  dragOverEventListeners: Map<HTMLElement, EventListener>;
  dragLeaveEventListeners: Map<HTMLElement, EventListener>;

  initTree(folder: Array<SyncDocument>): void;
  watchFolderState(): void;
  openDocument(folder: any): Promise<void>;
  setSwitchDisplayHandler(): void;
  // drag & drop actions
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
}

export const workspaceNextcloudFolderController = ng.controller(
  "NextcloudFolderController",
  [
    "$scope",
    "$rootScope",
    "NextcloudService",
    "NextcloudUserService",
    "NextcloudEventService",
    (
      $scope: INextcloudFolderScope,
      $rootScope: any,
      nextcloudService: INextcloudService,
      nextcloudUserService: INextcloudUserService,
      nextcloudEventService: INextcloudEventService,
    ) => {
      // Mirrors $rootScope.isGDTrashbinOpen: true once Nextcloud is the active section.
      $rootScope.isNextcloudTrashbinOpen = false;
      $scope.userInfo = null;
      $scope.documents = [];
      $scope.folderTree = {};
      $scope.selectedFolder = null;
      $scope.openedFolder = [];
      $scope.dragOverEventListeners = new Map<HTMLElement, EventListener>();
      $scope.dragLeaveEventListeners = new Map<HTMLElement, EventListener>();
      $scope.folderCreation = new FolderCreationModel($scope);
      $scope.emptyTrashbin = new EmptyTrashModel($scope);
      $scope.isTrashbinOpen = false;

      let subscriptions: Subscription = new Subscription();

      // Resolve user has the following actions:
      // 1. Fetch user info
      // 1.a) If user exists, fetch user info
      // 1.b) If user does not exist, it will create its nextcloud user
      nextcloudUserService
        .resolveUser(model.me.userId)
        .then((user) => {
          nextcloudUserService
            .getUserInfo(model.me.userId)
            .then(async (nextcloudUserInfo: UserNextcloud) => {
              $scope.userInfo = nextcloudUserInfo;
              $scope.documents = [new SyncDocument().initParent()];
              $scope.initTree($scope.documents);
              $scope.initDraggable();
              setTimeout(injectNextcloudRootGroupIcon, 0);

              const urlParams = new URLSearchParams(window.location.search);
              if (urlParams.get("folder") === "synced") {
                await $scope.openDocument($scope.documents[0]);
                await $scope.folderTree.openFolder($scope.documents[0]);
                $scope.setSwitchDisplayHandler();
                template.open("documents", "nextcloud/content/workspace-nextcloud-content");
                WorkspaceEntcoreUtils.toggleWorkspaceContentDisplay(false);
              }

              safeApply($scope);
            })
            .catch((err: Error) => {
              const message: string =
                "Error while attempting to fetch user info";
              console.error(message + err.message);
            });
        })
        .catch((err: Error) => {
          const message: string = "Error while attempting to resolve user";
          console.error(message + err.message);
        });

      // on receive openFolder event
      subscriptions.add(
        nextcloudEventService
          .getOpenedFolderDocument()
          .subscribe((document: SyncDocument) => {
            // trees() now returns a single "Nextcloud" root wrapper, so search its children instead.
            const roots: Array<SyncDocument> = $scope.folderTree.trees as any;
            let getFolderContext: SyncDocument = (roots?.[0]?.children ?? []).find(
              (f) => f.fileId === document.fileId,
            );
            $scope.folderTree.openFolder(
              getFolderContext ? getFolderContext : document,
            );
          }),
      );

      $scope.initTree = (folder: Array<SyncDocument>): void => {
        // use this const to make it accessible to its folderTree inner context
        const viewModel: INextcloudFolderScope = $scope;

        // move nextcloud tree under workspace tree
        const nextcloudElement: HTMLElement = document.querySelector(
          '[application="nextcloud"]',
        ).parentElement;
        if (nextcloudElement) {
          nextcloudElement.parentNode.appendChild(nextcloudElement);
        }

        // we create all the static folders
        const staticFolders: Array<SyncDocument> = [
          SyncDocument.createStaticFolder("trashbin"),
        ];

        // then we  add them to the folder tree
        folder.push(...staticFolders);

        // Wraps "Mes documents"/"Corbeille" under a single "Nextcloud" root — see SyncDocument.createRootGroup().
        const rootGroup = SyncDocument.createRootGroup();
        rootGroup.children = folder;
        // Open by default on page load, mirroring Google Drive's own root group.
        viewModel.openedFolder = [rootGroup as any];

        $scope.folderTree = {
          cssTree: "folders-tree",
          get trees(): any | Array<Tree> {
            return [rootGroup] as any;
          },
          isDisabled(folder: models.Element): boolean {
            return false;
          },
          isOpenedFolder(folder: models.Element): boolean {
            return viewModel.openedFolder.some(
              (openFolder: models.Element) => openFolder === folder,
            );
          },
          isSelectedFolder(folder: models.Element): boolean {
            return viewModel.selectedFolder === folder;
          },
          async openFolder(folder: models.Element): Promise<void> {
            // Clicking the "Nextcloud" grouping label toggles fold/unfold and also opens "Mes documents".
            if ((folder as any).isRootGroup) {
              if (viewModel.openedFolder.some((f) => f === folder)) {
                viewModel.openedFolder = viewModel.openedFolder.filter((f) => f !== folder);
              } else {
                viewModel.openedFolder.push(folder);
              }
              setTimeout(injectNextcloudRootGroupIcon, 0);
              return $scope.folderTree.openFolder(rootGroup.children[0] as any);
            }

            viewModel.selectedFolder = folder;
            // Clear Google Drive's own selectedFolder so its last-selected row doesn't stay highlighted.
            const gdTreeEl = document.getElementById("google-drive-folder-tree");
            const gdTreeScope: any = angular.element(gdTreeEl).scope();
            if (gdTreeScope) {
              gdTreeScope.selectedFolder = null;
              const phase = gdTreeScope.$root && gdTreeScope.$root.$$phase;
              if (!phase) {
                gdTreeScope.$apply();
              }
            }
            // Only clear "selected" — "opened" also drives expand/fold state, clearing it would collapse the tree.
            if (gdTreeEl) {
              gdTreeEl.querySelectorAll("a.selected").forEach((el) => {
                el.classList.remove("selected");
              });
            }
            viewModel.setSwitchDisplayHandler();
            // create handler in case icon are only clicked
            viewModel.watchFolderState();

            if (
              !viewModel.openedFolder.some(
                (openFolder: models.Element) => openFolder === folder,
              )
            ) {
              viewModel.openedFolder = viewModel.openedFolder.filter(
                (e: models.Element) => (<any>e).path != (<any>folder).path,
              );
              viewModel.openedFolder.push(folder);
            }

            await viewModel.openDocument(folder);
            setTimeout(injectNextcloudRootGroupIcon, 0);

            // reset drag feedback by security
            viewModel.removeDragFeedback();
            // init drag over
            viewModel.addDragFeedback();
          },
        };
      };

      $scope.initDraggable = (): void => {
        const viewModel: INextcloudFolderScope = $scope;
        $scope.droppable = {
          dragConditionHandler(event: DragEvent, content?: any): boolean {
            return false;
          },
          async dragDropHandler(event: DragEvent): Promise<void> {
            await viewModel.resolveDragTarget(event);
          },
          dragEndHandler(event: DragEvent, content?: any): void {
          },
          dragStartHandler(event: DragEvent, content?: any): void {
          },
          dropConditionHandler(event: DragEvent, content?: any): boolean {
            return false;
          },
        };
      };

      function removeDropTarget(event: DragEvent) {
        const target: HTMLElement = event.target as HTMLElement;
        const droppableElement: HTMLElement = target.closest('.folder-list-item') || target;
        if (droppableElement) {
          droppableElement.classList.remove("droptarget");
        }
      }

      $scope.resolveDragTarget = async (event: DragEvent): Promise<void> => {
        removeDropTarget(event);
        // case drop concerns nextcloud
        if (nextcloudEventService.getContentContext()) {
          //nextcloud context
        } else {
          // case drop concerns workspace but we need extra check
          const document: any = JSON.parse(
            event.dataTransfer.getData("application/json"),
          );
          // check if it s a workspace document with its identifier and format file to proceed move to nextcloud
          if (
            document &&
            ((document._id && document.eType === DocumentsType.FILE) ||
              document.eType === DocumentsType.FOLDER)
          ) {
            if (
              angular.element(event.target).scope().folder instanceof
              SyncDocument
            ) {
              const syncedDocument: SyncDocument = angular
                .element(event.target)
                .scope().folder;
              let selectedDocuments: Array<Document> =
                WorkspaceEntcoreUtils.workspaceScope()["documentList"][
                "_documents"
                ];
              selectedDocuments = selectedDocuments.concat(
                WorkspaceEntcoreUtils.workspaceScope()["currentTree"][
                "children"
                ],
              );
              let documentToUpdate: Set<string> = new Set(
                selectedDocuments
                  .filter((file: Document) => file.selected)
                  .map((file: Document) => file._id),
              );
              documentToUpdate.add(document._id);
              nextcloudService
                .moveDocumentWorkspaceToCloud(
                  model.me.userId,
                  Array.from(documentToUpdate),
                  syncedDocument.path,
                )
                .then((_: any) => {
                  WorkspaceEntcoreUtils.updateWorkspaceDocuments(
                    WorkspaceEntcoreUtils.workspaceScope()["openedFolder"][
                    "folder"
                    ],
                  );
                  nextcloudEventService.sendOpenFolderDocument(
                    angular.element(event.target).scope().folder,
                  );
                  $scope.selectedFolder = null;
                  angular
                    .element(event.target)
                    .scope()
                    .folder.classList.remove("selected");
                })
                .catch((err: Error) => {
                  const message: string =
                    "Error while attempting to fetch documents children ";
                  console.error(message + err.message);
                });
            }
          }
        }
      };

      $scope.watchFolderState = (): void => {
        // Get all folder tree arrow icons using vanilla JS
        const folderArrows = document.querySelectorAll(
          "#nextcloud-folder-tree i",
        );

        // Remove existing event listeners
        folderArrows.forEach((element) => {
          element.removeEventListener("click", onClickFolder($scope));
        });

        // Use this const to make it accessible to its callback
        const viewModel: INextcloudFolderScope = $scope;

        // Add new click event listeners to each folder arrow
        folderArrows.forEach((element) => {
          element.addEventListener("click", onClickFolder(viewModel));
        });
      };

      $scope.openDocument = async (document: any): Promise<void> => {
        if (document.isRootGroup) {
          // Pure grouping node ("Nextcloud" wrapping Mes documents/Corbeille) — nothing to load.
          return;
        }
        if ((<any>document).isStaticFolder) {
          const staticType: string = (<any>document).staticFolderType;
          let staticDocuments: Array<SyncDocument> = [];

          switch (staticType) {
            case "trashbin":
              $scope.isTrashbinOpen = true;
              $rootScope.isNextcloudTrashbinOpen = true;
              $rootScope.isGDTrashbinOpen = false;
              const trashList = await nextcloudService
                .listTrash(model.me.userId)
                .catch((err: Error) => {
                  const message: string = "Error while attempting to fetch  ";
                  console.error(message + err.message);
                  return [];
                });
              staticDocuments = trashList;
          }

          $scope.documents = staticDocuments;
          nextcloudEventService.sendDocuments({
            parentDocument: document,
            documents: staticDocuments,
          });
          safeApply($scope);
          return;
        }

        $scope.isTrashbinOpen = false;
        $rootScope.isNextcloudTrashbinOpen = true;
        $rootScope.isGDTrashbinOpen = false;

        let syncDocuments: Array<SyncDocument> = await nextcloudService
          .listDocument(model.me.userId, document.path ? document.path : null)
          .catch((err: Error) => {
            const message: string =
              "Error while attempting to fetch documents children ";
            console.error(message + err.message);
            return [];
          });
        // first filter applies only when we happen to fetch its own folder and the second applies on document only
        document.children = syncDocuments
          .filter(NextcloudDocumentsUtils.filterRemoveOwnDocument(document))
          .filter(NextcloudDocumentsUtils.filterDocumentOnly());
        safeApply($scope);
        nextcloudEventService.sendDocuments({
          parentDocument: document.path
            ? document
            : new SyncDocument().initParent(),
          documents: syncDocuments.filter(
            NextcloudDocumentsUtils.filterRemoveOwnDocument(document),
          ),
        });
      };

      $scope.setSwitchDisplayHandler = (): void => {
        const viewModel: INextcloudFolderScope = $scope;

        // case nextcloud folder tree is interacted
        // checking if listener does not exist in order to create one
        const nextcloudFolder = document.querySelector(
          "#nextcloud-folder-tree",
        );
        if (nextcloudFolder) {
          // Remove old event listener if exists
          const oldHandler = nextcloudFolder["workspaceNextcloudHandler"];
          if (oldHandler) {
            nextcloudFolder.removeEventListener("click", oldHandler);
          }

          // Create and store new handler
          const newHandler = switchWorkspaceTreeHandler();
          nextcloudFolder["workspaceNextcloudHandler"] = newHandler;
          nextcloudFolder.addEventListener("click", newHandler);
        }

        // case entcore workspace folder tree is interacted
        // we unbind its handler and rebind it in order to keep our list of workspace updated
        const workspaceTree = document.querySelector(
          WorkspaceEntcoreUtils.$ENTCORE_WORKSPACE,
        );
        if (workspaceTree) {
          // Remove old event listener if exists
          const oldHandler = workspaceTree["nextcloudHandler"];
          if (oldHandler) {
            workspaceTree.removeEventListener("click", oldHandler);
          }

          // Create and store new handler
          const newHandler = switchNextcloudTreeHandler(viewModel);
          workspaceTree["nextcloudHandler"] = newHandler;
          workspaceTree.addEventListener("click", newHandler);
        }
      };

      $scope.removeSelectedDocuments = (): void => {
        let selectedDocuments: Array<Document> =
          WorkspaceEntcoreUtils.workspaceScope()["openedFolder"]["documents"];
        let folders: Array<Document> =
          WorkspaceEntcoreUtils.workspaceScope()["openedFolder"]["folders"];
        if (selectedDocuments != null && folders != null) {
          selectedDocuments.forEach((doc: Document) => (doc.selected = false));
          folders.forEach((fol: Document) => (fol.selected = false));
        }
      };

      $scope.addDragEventListeners = (): void => {
        const folders: HTMLElement[] = Array.from(
          document.getElementsByTagName("folder-tree-inner"),
        ) as HTMLElement[];
        folders.forEach((element: HTMLElement) => {
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
        folders.forEach((element: HTMLElement, i: number) => {
          const span: HTMLElement = document.createElement("span");
          span.id = "droptarget-" + i;
          span.className = "highlight-title highlight-title-border ng-scope";
          const subSpan: HTMLElement = document.createElement("span");
          subSpan.className = "count-badge ng-binding";
          span.appendChild(subSpan);

          const ul: Element = element.lastElementChild;
          if (ul.tagName === "UL") {
            element.insertBefore(span, ul);
          } else {
            element.appendChild(span);
          }
          element.style.position = "relative";
          element.style.display = "block";
        });
      };

      $scope.removeDragOverlays = (): void => {
        const spans: HTMLElement[] = Array.from(
          document.querySelectorAll(`[id^="droptarget-"]`),
        ) as HTMLElement[];
        spans.forEach((element: HTMLElement) => {
          element.remove();
        });
      };

      $scope.removeDragEventListeners = (): void => {
        $scope.dragOverEventListeners.forEach(
          (listener: EventListener, element: HTMLElement) =>
            element.removeEventListener("dragover", onDragOver(element)),
        );
        $scope.dragOverEventListeners.clear();
        $scope.dragLeaveEventListeners.forEach(
          (listener: EventListener, element: HTMLElement) =>
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
      };

      function onDragLeave(element: HTMLElement): EventListener {
        return function (event: Event): void {
          event.preventDefault();
          event.stopPropagation();
          element.firstElementChild.classList.remove("droptarget");
        };
      }

      function onDragOver(element: HTMLElement): EventListener {
        return function (event: Event): void {
          // A Google Drive-sourced drag has no transfer path into Nextcloud — leave the
          // browser's default "no-drop" cursor instead of highlighting this as a valid target.
          if (googleDriveEventService.getContentContext()) return;
          event.preventDefault();
          event.stopPropagation();
          element.firstElementChild.classList.add("droptarget");
        };
      }

      // <folder-tree-inner> has no icon slot — icons are injected as plain DOM nodes after render.
      const FOLDER_ICON_SVG: string =
        '<path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>';
      const FOLDER_OPEN_ICON_SVG: string =
        '<path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M2 8V6a2 2 0 0 1 2-2h4.5l2 2H20a2 2 0 0 1 2 2"/>' +
        '<path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M2 8h19a1 1 0 0 1 .97 1.24l-1.5 6A2 2 0 0 1 18.53 17H4.5a2 2 0 0 1-1.94-1.51L1 9.5A1 1 0 0 1 2 8Z"/>';
      const TRASH_ICON_SVG: string =
        '<path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6zM10 11v6M14 11v6"/>';
      // Official Nextcloud mark, recolored to brand blue to match Google Drive's colored root icon.
      const NEXTCLOUD_ROOT_SVG: string =
        '<path fill="#0082c9" d="m128 7c-25.871 0-47.817 17.485-54.713 41.209-5.9795-12.461-18.642-21.209-33.287-21.209-20.304 0-37 16.696-37 37s16.696 37 37 37c14.645 0 27.308-8.7481 33.287-21.209 6.8957 23.724 28.842 41.209 54.713 41.209s47.817-17.485 54.713-41.209c5.9795 12.461 18.642 21.209 33.287 21.209 20.304 0 37-16.696 37-37s-16.696-37-37-37c-14.645 0-27.308 8.7481-33.287 21.209-6.8957-23.724-28.842-41.209-54.713-41.209zm0 22c19.46 0 35 15.54 35 35s-15.54 35-35 35-35-15.54-35-35 15.54-35 35-35zm-88 20c8.4146 0 15 6.5854 15 15s-6.5854 15-15 15-15-6.5854-15-15 6.5854-15 15-15zm176 0c8.4146 0 15 6.5854 15 15s-6.5854 15-15 15-15-6.5854-15-15 6.5854-15 15-15z"/>';

      function prependIcon(
        link: Element,
        innerSvg: string,
        markerClass: string,
        viewBox: string = "0 0 24 24",
        replaceMarkerClasses: Array<string> = [],
      ): void {
        if (!link) return;
        (link as HTMLElement).style.setProperty("white-space", "nowrap", "important");
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

      function injectNextcloudRootGroupIcon(): void {
        const treeEl = document.getElementById("nextcloud-folder-tree");
        if (!treeEl) return;

        const links = treeEl.querySelectorAll("folder-tree-inner > a.folder-list-item");
        links.forEach((link) => {
          const innerEl: any = link.closest("folder-tree-inner");
          if (!innerEl) return;
          const innerJq: any = angular.element(innerEl);
          const scope: any = innerJq.isolateScope?.() ?? innerJq.scope?.();
          const folder: SyncDocument = scope?.folder;
          if (!folder) return;

          if (folder.isRootGroup) {
            prependIcon(link, NEXTCLOUD_ROOT_SVG, "nextcloud-root-icon", "0 0 256 128");
          } else if (folder.staticFolderType === "trashbin") {
            prependIcon(link, TRASH_ICON_SVG, "nextcloud-child-icon");
          } else {
            const isOpen = $scope.openedFolder?.some((f: any) => f === folder) ?? false;
            if (isOpen) {
              prependIcon(link, FOLDER_OPEN_ICON_SVG, "nextcloud-child-icon-open", "0 0 24 24", [
                "nextcloud-child-icon",
                "nextcloud-child-icon-open",
              ]);
            } else {
              prependIcon(link, FOLDER_ICON_SVG, "nextcloud-child-icon", "0 0 24 24", [
                "nextcloud-child-icon",
                "nextcloud-child-icon-open",
              ]);
            }
          }
        });
      }

      function switchWorkspaceTreeHandler() {
        const viewModel: INextcloudFolderScope = $scope;
        return function (): void {
          if (!viewModel.selectedFolder) {
            viewModel.folderTree.openFolder(viewModel.documents[0]);
          }

          const workspaceFolderTree = document.querySelectorAll(
            WorkspaceEntcoreUtils.$ENTCORE_WORKSPACE + " li a",
          );
          // using nextcloud content display
          template.open(
            "documents",
            `nextcloud/content/workspace-nextcloud-content`,
          );

          viewModel.removeSelectedDocuments();

          // clear all potential "selected" class workspace folder tree
          workspaceFolderTree.forEach((element: Element): void => {
            element.classList.remove("selected");
          });

          // hide workspace contents (search bar, menu, list of folder/files...) interactions
          WorkspaceEntcoreUtils.toggleWorkspaceContentDisplay(false);
        };
      }

      function switchNextcloudTreeHandler(viewModel: INextcloudFolderScope) {
        return function (): void {
          let element: Element = arguments[0].target;
          let target: Element;
          if (element && element.tagName === "A") {
            target = element;
          } else if (
            element &&
            element.parentElement &&
            element.parentElement.tagName === "A"
          ) {
            target = element.parentElement;
          }

          if (target && viewModel.selectedFolder) {
            const classicScope: any = WorkspaceEntcoreUtils.workspaceScope();
            let folder: any = angular.element(target).scope().folder;
            // "Nextcloud" is a pure grouping header; redirect so its children aren't treated as documents inside it.
            const wrapperRoot = classicScope?.wrapperTrees?.[0];
            const redirectedFromWrapper = folder === wrapperRoot;
            if (redirectedFromWrapper) {
              folder = (classicScope?.trees || []).find((t: any) => t.filter === "owner") ?? folder;
            }
            // go back to workspace content display
            // clear nextCloudTree interaction
            viewModel.selectedFolder = null;
            $rootScope.isNextcloudTrashbinOpen = false;
            if (!redirectedFromWrapper) {
              target.classList.add("selected");
            }
            // update workspace folder content
            WorkspaceEntcoreUtils.updateWorkspaceDocuments(folder);
            //set the right openedFolder
            WorkspaceEntcoreUtils.workspaceScope()["openedFolder"]["folder"] = folder;
            // display workspace contents (search bar, menu, list of folder/files...) interactions
            WorkspaceEntcoreUtils.toggleWorkspaceContentDisplay(true);
            // remove any content context cache
            nextcloudEventService.setContentContext(null);
            template.open("documents", `icons`);
          }
        };
      }

      function onClickFolder(viewModel: INextcloudFolderScope) {
        return function () {
          event.stopPropagation();
          const scope: any = angular.element(arguments[0].target).scope();
          const folder: models.Element = scope.folder;
          if (
            viewModel.openedFolder.some(
              (openFolder: models.Element) => openFolder === folder,
            )
          ) {
            viewModel.openedFolder = viewModel.openedFolder.filter(
              (openedFolder: models.Element) => openedFolder !== folder,
            );
          } else {
            viewModel.openedFolder.push(folder);
          }
          safeApply(scope);
        };
      }
    },
  ],
);

export const workspaceNextcloudFolder = ng.directive(
  "workspaceNextcloudFolder",
  () => {
    return {
      restrict: "E",
      templateUrl:
        "/workspace/public/template/nextcloud/folder/workspace-nextcloud-folder.html",
      controller: "NextcloudFolderController",
    };
  },
);
