/*
 * ⚠️ WARNING ⚠️
 * This component is almost an exact copy of the folderPicker component
 * with a few adaptations to make it work with nextcloud.
 */

import { ng } from "entcore";
import { models, workspaceService } from "../services";
import { FolderTreeProps } from "./folderTree2";
import { SyncDocument } from "./nextcloud/models/nextcloudFolder.model";

export interface FolderPickerScope {
  treeProps: FolderTreeProps<models.Tree>;
  nextcloudTreeProps: FolderTreeProps<SyncDocument>;
  folderProps: FolderPickerProps;

  // Tree data
  trees: models.Tree[];
  nextcloudTrees: SyncDocument[];

  // UI state
  selectedFolder: models.Element | SyncDocument;
  newFolder: models.Element;
  search: {
    value: string;
  };

  // Utility methods
  safeApply(fn?: () => void): void;

  // State check methods
  isStateNormal(): boolean;
  isStateLoading(): boolean;
  isStateLoaded(): boolean;
  isStateEmpty(): boolean;

  // New folder operations
  openEditView(): void;
  canOpenEditView(): boolean;
  isEditVew(): boolean;
  submitNewFolder(): void;

  // Search operations
  canResetSearch(): boolean;
  resetSearch(): void;
  searchKeyUp(event: KeyboardEvent): void;

  // Action methods
  onError(error?: any): void;
  onCancel(): void;
  onSubmit(): void;
  cannotSubmit(): boolean;
}

export interface FolderPickerSource {
  action: "create-from-blob" | "copy-from-file" | "move-from-file";
}

export interface FolderPickerSourceFile extends FolderPickerSource {
  fileId: string;
}

export interface FolderPickerSourceBlob extends FolderPickerSource {
  title?: string;
  content: Blob;
}

export interface FolderPickerProps {
  i18: {
    title: string;
    actionTitle: string;
    actionProcessing: string;
    actionFinished: string;
    actionEmpty?: string;
    info: string;
  };
  sources: FolderPickerSource[];
  treeProvider?(): Promise<models.Tree[]>;
  nextcloudTreeProvider?(): Promise<SyncDocument[]>;
  manageSubmit?(folder: models.Element | SyncDocument): boolean;
  submit?(folder: models.Element | SyncDocument): Promise<void> | void;
  onCancel(): void;
  onError?(error: any): void;
  onSubmitSuccess?(dest: models.Element | SyncDocument, count: number): void;
}

export const folderPicker2 = ng.directive("folderPicker2", [
  "$timeout",
  () => {
    return {
      restrict: "E",
      scope: {
        folderProps: "=",
      },
      template: `
        <div class="horizontal-spacing-twice">
          <h2 translate content="[[folderProps.i18.title]]"></h2>
          <div>
            <div class="row" ng-if="folderProps.i18.info">
              <div class="info" translate content="[[folderProps.i18.info]]"></div>
            </div>
            <div class="row top-spacing-twice">
              <!-- Workspace tree -->
              <div class="tree-container workspace-tree">
                <folder-tree-2 tree-props="treeProps"></folder-tree-2>
              </div>
              <!-- Nextcloud tree -->
              <div class="tree-container nextcloud-tree" ng-if="folderProps.nextcloudTreeProvider">
                <folder-tree-2 tree-props="nextcloudTreeProps"></folder-tree-2>
              </div>
              <hr />
              <div class="lightbox-buttons fluid">
                  <button class="nextcloud-button-confirm right-magnet" ng-disabled="cannotSubmit()" ng-click="onSubmit()" translate content="[[folderProps.i18.actionTitle]]"></button>
                  <button class="nextcloud-button-cancel cancel right-magnet" ng-click="onCancel()"><i18n>cancel</i18n></button>
              </div>
            </div>
          </div>
        </div>
      `,
      link: async (scope: FolderPickerScope) => {
        scope.search = { value: "" };
        scope.newFolder = new models.Element();

        scope.safeApply = function (fn) {
          const phase = this.$root.$$phase;
          if (phase == "$apply" || phase == "$digest") {
            if (fn && typeof fn === "function") {
              fn();
            }
          } else {
            this.$apply(fn);
          }
        };

        scope.selectedFolder = null;

        scope.trees = [];
        scope.nextcloudTrees = [];

        const canSelect = function (folder: models.Element | SyncDocument) {
          if (folder instanceof models.Element) {
            if ((folder as models.Tree).filter) {
              return (folder as models.Tree).filter == "owner";
            } else {
              return true;
            }
          } else if (folder instanceof SyncDocument) {
            return folder.isFolder;
          }
          return false;
        };

        let selectedWorkspaceFolder: models.Element = null;
        let openedWorkspaceFolder: models.Element = null;

        scope.treeProps = {
          cssTree: "maxheight-half-vh",
          get trees() {
            return scope.trees;
          },
          isDisabled(folder: models.Element) {
            return !canSelect(folder);
          },
          isOpenedFolder(folder: models.Element) {
            if (openedWorkspaceFolder === folder) {
              return true;
            } else if ((folder as models.Tree).filter) {
              if (!workspaceService.isLazyMode()) {
                return true;
              }
            }
            return (
              openedWorkspaceFolder &&
              workspaceService.findFolderInTreeByRefOrId(
                folder,
                openedWorkspaceFolder,
              )
            );
          },
          isSelectedFolder(folder: models.Element) {
            return selectedWorkspaceFolder === folder;
          },
          openFolder(folder: models.Element) {
            selectedNextcloudFolder = null;
            openedNextcloudFolders = [];

            if (canSelect(folder)) {
              openedWorkspaceFolder = selectedWorkspaceFolder = folder;
              scope.selectedFolder = folder;
            } else {
              openedWorkspaceFolder = folder;
            }
          },
        };

        // Nextcloud tree props (reuse workspaceNextcloudFolder logic)
        let selectedNextcloudFolder: SyncDocument = null;
        let openedNextcloudFolders: SyncDocument[] = [];

        scope.nextcloudTreeProps = {
          cssTree: "maxheight-half-vh",
          get trees() {
            return scope.nextcloudTrees;
          },
          isDisabled(folder: SyncDocument) {
            return false;
          },
          isOpenedFolder(folder: SyncDocument) {
            return openedNextcloudFolders.some(
              (openFolder) => openFolder === folder,
            );
          },
          isSelectedFolder(folder: SyncDocument) {
            return selectedNextcloudFolder === folder;
          },
          openFolder(folder: SyncDocument) {
            // Clear workspace selection
            selectedWorkspaceFolder = null;
            openedWorkspaceFolder = null;

            selectedNextcloudFolder = folder;
            scope.selectedFolder = folder;

            // Add to opened folders if not already there
            if (!openedNextcloudFolders.includes(folder)) {
              openedNextcloudFolders.push(folder);
            }
          },
        };

        // Load trees
        const loadTrees = async () => {
          try {
            // Load workspace trees
            if (scope.folderProps.treeProvider) {
              const workspaceTrees = await scope.folderProps.treeProvider();
              if (workspaceTrees && workspaceTrees.length > 0) {
                scope.trees.length = 0;
                workspaceTrees.forEach((tree) => scope.trees.push(tree));
              }
            }

            // Load Nextcloud trees
            if (scope.folderProps.nextcloudTreeProvider) {
              const nextcloudTrees =
                await scope.folderProps.nextcloudTreeProvider();
              if (nextcloudTrees && nextcloudTrees.length > 0) {
                scope.nextcloudTrees.length = 0;
                nextcloudTrees.forEach((tree) =>
                  scope.nextcloudTrees.push(tree),
                );
              }
            }

            scope.safeApply();
          } catch (e) {
            console.error("Error loading trees:", e);
            if (scope.folderProps.onError) {
              scope.folderProps.onError(e);
            }
          }
        };

        // Load trees
        await loadTrees();

        // Submission handling
        scope.cannotSubmit = () => !scope.selectedFolder;

        scope.onSubmit = async () => {
          if (scope.cannotSubmit()) return;

          // If custom submit handling is provided, use that
          if (
            scope.folderProps.manageSubmit &&
            scope.folderProps.manageSubmit(scope.selectedFolder)
          ) {
            return;
          }

          try {
            if (scope.folderProps.submit) {
              await scope.folderProps.submit(scope.selectedFolder);
              scope.safeApply();
            }
          } catch (e) {
            scope.folderProps.onError(e);
            scope.safeApply();
          }
        };

        scope.onCancel = () => {
          scope.folderProps.onCancel();
        };
      },
    };
  },
]);
