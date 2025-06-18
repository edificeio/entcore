import {
  angular,
  Document,
  FolderTreeProps,
  model,
  ng,
  template,
} from "entcore";
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
    "NextcloudService",
    "NextcloudUserService",
    "NextcloudEventService",
    (
      $scope: INextcloudFolderScope,
      nextcloudService: INextcloudService,
      nextcloudUserService: INextcloudUserService,
      nextcloudEventService: INextcloudEventService,
    ) => {
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

      nextcloudUserService
        .getUserInfo(model.me.userId)
        .then((nextcloudUserInfo: UserNextcloud) => {
          $scope.userInfo = nextcloudUserInfo;
          $scope.documents = [new SyncDocument().initParent()];
          $scope.initTree($scope.documents);
          $scope.initDraggable();
          safeApply($scope);
        })
        .catch((err: Error) => {
          const message: string = "Error while attempting to fetch user info";
          console.error(message + err.message);
        });

      // on receive openFolder event
      subscriptions.add(
        nextcloudEventService
          .getOpenedFolderDocument()
          .subscribe((document: SyncDocument) => {
            let getFolderContext: SyncDocument = $scope.folderTree.trees.find(
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

        $scope.folderTree = {
          cssTree: "folders-tree",
          get trees(): any | Array<Tree> {
            return folder;
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
            viewModel.selectedFolder = folder;
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

            if ((<any>folder).isStaticFolder) {
              await viewModel.openDocument(folder);
            } else {
              // synchronize documents and send content to its other sniplet content
              await viewModel.openDocument(folder);
            }

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
          dragEndHandler(event: DragEvent, content?: any): void {},
          dragStartHandler(event: DragEvent, content?: any): void {},
          dropConditionHandler(event: DragEvent, content?: any): boolean {
            return false;
          },
        };
      };

      function removeDropTarget(event:DragEvent) {
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
        if ((<any>document).isStaticFolder) {
          const staticType: string = (<any>document).staticFolderType;
          let staticDocuments: Array<SyncDocument> = [];

          switch (staticType) {
            case "trashbin":
              $scope.isTrashbinOpen = true;
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
          event.preventDefault();
          event.stopPropagation();
          element.firstElementChild.classList.add("droptarget");
        };
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
            // go back to workspace content display
            // clear nextCloudTree interaction
            viewModel.selectedFolder = null;
            target.classList.add("selected");
            // update workspace folder content
            WorkspaceEntcoreUtils.updateWorkspaceDocuments(
              angular.element(target).scope().folder,
            );
            //set the right openedFolder
            WorkspaceEntcoreUtils.workspaceScope()["openedFolder"]["folder"] =
              angular.element(target).scope().folder;
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
