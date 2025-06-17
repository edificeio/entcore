import { AxiosError, AxiosResponse } from "axios";
import { angular, Me, model, ng, template, workspace } from "entcore";
import { Subscription } from "rxjs";
import { ViewMode } from "../../enums/viewMode.enum";
import { Draggable } from "../../models/nextcloudDraggable.model";
import { SyncDocument } from "../../models/nextcloudFolder.model";
import { INextcloudEventService } from "../../services/nextcloudEvent.service";
import { nextcloudUserService } from "../../services/nextcloudUser.service";
import {
  NextcloudPreference,
  Preference,
} from "../../services/nextcloud.preferences";
import { INextcloudService } from "../../services/nextcloud.service";
import { safeApply } from "../../utils/safeApply.utils";
import { UploadFileSnipletViewModel } from "./fileUpload.component";
import { NextcloudViewIcons } from "./iconView.component";
import { INextcloudViewList, NextcloudViewList } from "./listView.component";
import { ToolbarSnipletViewModel } from "./toolbar.component";
import models = workspace.v2.models;

declare let window: any;

const nextcloudTree: string = "nextcloud-folder-tree";

export interface IWorkspaceNextcloudContent {
  safeApply(): void;
  initDraggable(): void;
  onSelectContent(document: SyncDocument): void;
  onSelectAll(): void;
  onOpenContent(document: SyncDocument): void;
  getFile(document: SyncDocument): string;
  nextcloudUrl: string;
  isNextcloudUrlHidden: boolean;
  draggable: Draggable;
  lockDropzone: boolean;
  parentDocument: SyncDocument;
  documents: Array<SyncDocument>;
  selectedDocuments: Array<SyncDocument>;
  checkboxSelectAll: boolean;
  // drag & drop action
  moveDocument(element: any, document: SyncDocument): Promise<void>;
  // dropzone
  isDropzoneEnabled(): boolean;
  canDropOnFolder(): boolean;
  onCannotDropFile(): void;
  isViewMode(mode: ViewMode): boolean;
  changeViewMode(mode: ViewMode): Promise<void>;

  isLoaded: boolean;
  viewIcons: NextcloudViewIcons;
  viewList: INextcloudViewList;
  toolbar: ToolbarSnipletViewModel;
  upload: UploadFileSnipletViewModel;
  updateTree(): void;
  getNextcloudTreeController(): any;

  isTrashMode(): boolean;

  openDocument(document?: SyncDocument): any;
  closeViewFile(): void;
}

export const workspaceNextcloudContentController = ng.controller(
  "NextcloudContentController",
  [
    "$scope",
    "NextcloudService",
    "NextcloudEventService",
    (
      $scope: IWorkspaceNextcloudContent,
      nextcloudService: INextcloudService,
      nextcloudEventService: INextcloudEventService,
    ) => {
      $scope.isLoaded = false;
      $scope.documents = [];
      $scope.parentDocument = null;
      $scope.nextcloudUrl = null;
      $scope.selectedDocuments = new Array<SyncDocument>();
      // fetch nextcloud url hidden state in order to hide or show the nextcloud url
      $scope.isNextcloudUrlHidden = false;

      let nextcloudPreference = new Preference();
      let orderDesc: boolean = false;
      let orderField: string = null;
      let subscription = new Subscription();

      $scope.getNextcloudTreeController = function () {
        return angular.element(document.getElementById(nextcloudTree)).scope();
      };

      $scope.isTrashMode = function (): boolean {
        const treeController = $scope.getNextcloudTreeController();
        return treeController?.isTrashbinOpen;
      };

      nextcloudService
        .getIsNextcloudUrlHidden()
        .then((isHidden) => ($scope.isNextcloudUrlHidden = isHidden))
        .catch((err: AxiosError) => {
          const message: string =
            "Error while attempting to fetch nextcloud url hidden state";
          console.error(message + err.message);
          $scope.isNextcloudUrlHidden = false;
        });

      // on init we first sync its main folder content
      Promise.all([
        initDocumentsContent(nextcloudService, $scope),
        nextcloudService.getNextcloudUrl(),
        nextcloudPreference.init(),
      ])
        .then(([_, url]) => {
          $scope.changeViewMode(nextcloudPreference.viewMode);
          $scope.nextcloudUrl = url;
          $scope.viewList = new NextcloudViewList($scope);
          $scope.viewIcons = new NextcloudViewIcons($scope);
          $scope.toolbar = new ToolbarSnipletViewModel($scope);
          $scope.upload = new UploadFileSnipletViewModel($scope);
          $scope.isLoaded = true;
          safeApply($scope);
        })
        .catch((err: AxiosError) => {
          const message: string =
            "Error while attempting to init or fetch nextcloud url: ";
          console.error(message + err.message);
          $scope.isLoaded = true;
          safeApply($scope);
        });

      // on receive documents from folder-tree sniplet
      subscription.add(
        nextcloudEventService
          .getDocumentsState()
          .subscribe(
            (res: {
              parentDocument: SyncDocument;
              documents: Array<SyncDocument>;
            }) => {
              if (res.documents && res.documents.length > 0) {
                $scope.parentDocument = res.parentDocument;

                // if we are in trash mode, we do not need to filter documents
                if ($scope.isTrashMode()) {
                  $scope.documents = res.documents.sort(sortDocumentsByFolder);
                } else {
                  $scope.documents = res.documents
                    .filter(
                      (syncDocument: SyncDocument) =>
                        syncDocument.name != model.me.userId,
                    )
                    .sort(sortDocumentsByFolder);
                  orderDesc = false;
                  orderField = "";
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

      initDraggable();

      async function initDocumentsContent(
        nextcloudService: INextcloudService,
        scope: IWorkspaceNextcloudContent,
      ): Promise<void> {
        // if we are in trash mode, we do not need to fetch documents
        if ($scope.isTrashMode()) {
          return;
        }

        let selectedFolderFromNextcloudTree: SyncDocument =
          $scope.getNextcloudTreeController()["selectedFolder"];
        return nextcloudService
          .listDocument(model.me.userId, selectedFolderFromNextcloudTree.path)
          .then((documents: Array<SyncDocument>) => {
            // will be called first time while constructor initializing
            // since it will syncing at the same time observable will receive its events, we check its length at the end
            if (!scope.documents.length) {
              scope.documents = documents
                .filter(
                  (syncDocument: SyncDocument) =>
                    syncDocument.path != selectedFolderFromNextcloudTree.path,
                )
                .filter(
                  (syncDocument: SyncDocument) =>
                    syncDocument.name != model.me.userId,
                )
                .sort(sortDocumentsByFolder);
              scope.parentDocument = new SyncDocument().initParent();
            }
            safeApply(scope);
          })
          .catch((err: AxiosError) => {
            const message: string =
              "Error while attempting to fetch documents children from content";
            console.error(message + err.message);
            return [];
          });
      }

      function updateTree(): void {
        const selectedFolderFromNextcloudTree: SyncDocument =
          $scope.getNextcloudTreeController()["selectedFolder"];
        updateFolderDocument(selectedFolderFromNextcloudTree);
        safeApply($scope);
      }

      function initDraggable(): void {
        // use this const to make it accessible to its folderTree inner context
        const viewModel = $scope;
        $scope.draggable = {
          dragConditionHandler(event: DragEvent, content?: any): boolean {
            return false;
          },
          dragDropHandler(event: DragEvent, content?: any): void {},
          async dragEndHandler(event: DragEvent, content?: any): Promise<void> {
            await viewModel.moveDocument(
              document.elementFromPoint(event.x, event.y),
              content,
            );
            viewModel.lockDropzone = false;
            safeApply($scope);
          },
          dragStartHandler(event: DragEvent, content?: any): void {
            viewModel.lockDropzone = true;
            try {
              event.dataTransfer.setData(
                "application/json",
                JSON.stringify(content),
              );
            } catch (e) {
              event.dataTransfer.setData("Text", JSON.stringify(content));
            }
            nextcloudEventService.setContentContext(content);
          },
          dropConditionHandler(event: DragEvent, content?: any): boolean {
            return true;
          },
        };
      }

      function sortDocumentsByFolder(
        syncDocumentA: SyncDocument,
        syncDocumentB: SyncDocument,
      ): number {
        if (syncDocumentA.type === "folder" && syncDocumentB.type === "file")
          return -1;
        if (syncDocumentA.type === "file" && syncDocumentB.type === "folder")
          return 1;
        return 0;
      }

      $scope.moveDocument = async function (
        element: any,
        document: SyncDocument,
      ): Promise<void> {
        let selectedFolderFromNextcloudTree: SyncDocument =
          $scope.getNextcloudTreeController()["selectedFolder"];
        if (!selectedFolderFromNextcloudTree) {
          selectedFolderFromNextcloudTree = $scope.parentDocument;
        }
        let folderContent: any = angular.element(element).scope();
        // if interacted into trees(workspace or nextcloud)
        if (folderContent && folderContent.folder) {
          processMoveTree(
            folderContent,
            document,
            selectedFolderFromNextcloudTree,
          );
        }
        if (
          folderContent &&
          folderContent.content instanceof SyncDocument &&
          folderContent.content.isFolder
        ) {
          // if interacted into nextcloud
          processMoveToNextcloud(
            document,
            folderContent.content,
            selectedFolderFromNextcloudTree,
          );
        }
      };

      function processMoveTree(
        folderContent: any,
        document: SyncDocument,
        selectedFolderFromNextcloudTree: SyncDocument,
      ): void {
        if (folderContent.folder instanceof models.Element) {
          const nextcloudController: any = $scope.getNextcloudTreeController();
          const filesToMove: Set<SyncDocument> = new Set(
            $scope.selectedDocuments,
          ).add(document);
          const filesPath: Array<string> = Array.from(filesToMove).map(
            (file: SyncDocument) => file.path,
          );
          if (filesPath.length) {
            nextcloudService
              .moveDocumentNextcloudToWorkspace(
                model.me.userId,
                filesPath,
                folderContent.folder._id,
              )
              .then(() => nextcloudUserService.getUserInfo(model.me.userId))
              .then((userInfos) => {
                nextcloudController.userInfo = userInfos;
                return nextcloudService.listDocument(
                  model.me.userId,
                  selectedFolderFromNextcloudTree.path
                    ? selectedFolderFromNextcloudTree.path
                    : null,
                );
              })
              .then((syncedDocument: Array<SyncDocument>) => {
                $scope.documents = syncedDocument
                  .filter(
                    (syncDocument: SyncDocument) =>
                      syncDocument.path != selectedFolderFromNextcloudTree.path,
                  )
                  .filter(
                    (syncDocument: SyncDocument) =>
                      syncDocument.name != model.me.userId,
                  );
                updateFolderDocument(selectedFolderFromNextcloudTree);
                safeApply($scope);
              })
              .catch((err: AxiosError) => {
                const message: string =
                  "Error while attempting to move nextcloud document to workspace " +
                  "or update nextcloud list";
                console.error(message + err.message);
              });
          }
        } else {
          processMoveToNextcloud(
            document,
            folderContent.folder,
            selectedFolderFromNextcloudTree,
          );
        }
      }

      async function moveAllDocuments(
        document: SyncDocument,
        target: SyncDocument,
      ): Promise<AxiosResponse[]> {
        const promises: Array<Promise<AxiosResponse>> = [];
        $scope.selectedDocuments.push(document);
        const selectedSet: Set<SyncDocument> = new Set(
          $scope.selectedDocuments,
        );
        selectedSet.forEach((doc: SyncDocument) => {
          if (doc.path != target.path) {
            promises.push(
              nextcloudService.moveDocument(
                model.me.userId,
                doc.path,
                (target.path != null ? target.path : "") + encodeURI(doc.name),
              ),
            );
          }
        });
        return await Promise.all<AxiosResponse>(promises);
      }

      function updateDocList(
        selectedFolderFromNextcloudTree: SyncDocument,
      ): void {
        $scope.selectedDocuments = [];
        nextcloudService
          .listDocument(
            model.me.userId,
            selectedFolderFromNextcloudTree.path
              ? selectedFolderFromNextcloudTree.path
              : null,
          )
          .then((syncedDocument: Array<SyncDocument>) => {
            $scope.documents = syncedDocument
              .filter(
                (syncDocument: SyncDocument) =>
                  syncDocument.path != selectedFolderFromNextcloudTree.path,
              )
              .filter(
                (syncDocument: SyncDocument) =>
                  syncDocument.name != model.me.userId,
              );
            updateFolderDocument(selectedFolderFromNextcloudTree);
            safeApply($scope);
          })
          .catch((err: AxiosError) => {
            const message: string = "Error while updating documents list";
            console.error(message + err.message);
          });
      }

      function processMoveToNextcloud(
        document: SyncDocument,
        target: SyncDocument,
        selectedFolderFromNextcloudTree: SyncDocument,
      ): void {
        moveAllDocuments(document, target)
          .then(() => updateDocList(selectedFolderFromNextcloudTree))
          .catch((err: AxiosError) => {
            updateDocList(selectedFolderFromNextcloudTree);
            const message: string =
              "Error while attempting to move nextcloud document to workspace " +
              "or update nextcloud list";
            console.error(message + err.message);
          });
      }

      function updateFolderDocument(
        selectedFolderFromNextcloudTree: SyncDocument,
      ): void {
        nextcloudEventService.setContentContext(null);
        nextcloudEventService.sendOpenFolderDocument(
          selectedFolderFromNextcloudTree,
        );
      }

      $scope.onSelectContent = function (content: SyncDocument): void {
        $scope.selectedDocuments = $scope.documents.filter(
          (document: SyncDocument) => document.selected,
        );
      };

      $scope.onSelectAll = function (): void {
        $scope.checkboxSelectAll = !$scope.checkboxSelectAll;
        $scope.documents.map(document => document.selected = $scope.checkboxSelectAll);

        $scope.selectedDocuments = $scope.documents.filter(
          (document: SyncDocument) => document.selected,
        );
      };

      $scope.isViewMode = function (mode: ViewMode): boolean {
        const pathTemplate = `nextcloud/content/views/${mode}`;
        return template.contains("documents-content", pathTemplate);
      };

      $scope.changeViewMode = async function (mode: ViewMode): Promise<void> {
        let preference: NextcloudPreference = Me.preferences["nextcloud"];
        preference.viewMode = mode;
        await nextcloudPreference.updatePreference(preference);
        const pathTemplate = `nextcloud/content/views/${mode}`;
        $scope.documents.forEach((document) => (document.selected = false));
        $scope.selectedDocuments = [];
        template.open("documents-content", pathTemplate);

        safeApply($scope);
      };

      $scope.openDocument = function(document?: SyncDocument): any {
        const pathTemplate: string = `nextcloud/content/views/viewer`;
        
        this.viewFile = document ? document : $scope.selectedDocuments[0];
        template.open("documents-content", pathTemplate);
        $scope.selectedDocuments = [];
      }

      $scope.closeViewFile = function(): any {
        let preference: NextcloudPreference = Me.preferences["nextcloud"];
        this.viewFile = null;
        $scope.changeViewMode(preference.viewMode);
      }

      $scope.onOpenContent = function (document: SyncDocument): void {
        if (document.isFolder) {
          nextcloudEventService.sendOpenFolderDocument(document);
          // reset all selected documents switch we switch folder
          $scope.selectedDocuments = [];
        } else {
            $scope.openDocument(document);
        }
      };

      $scope.getFile = function (document: SyncDocument): string {
        return nextcloudService.getFile(
          model.me.userId,
          document.name,
          document.path,
          document.contentType,
        );
      };

      $scope.isDropzoneEnabled = function (): boolean {
        return !$scope.lockDropzone;
      };

      $scope.canDropOnFolder = function (): boolean {
        return true;
      };

      $scope.onCannotDropFile = function (): void {};
    },
  ],
);

export const workspaceNextcloudContent = ng.directive(
  "workspaceNextcloudContent",
  () => {
    return {
      restrict: "E",
      templateUrl:
        "/workspace/public/template/nextcloud/content/workspace-nextcloud-content.html",
      controller: "NextcloudContentController",
    };
  },
);
