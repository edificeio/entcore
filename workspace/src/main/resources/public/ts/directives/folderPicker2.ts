/*
 * Enhanced version of folderPicker component that supports both local and Nextcloud trees
 */

import { ng } from "entcore";
import { models } from "../services";
import { FolderTreeProps } from "./folderTree2";
import { SyncDocument } from "./nextcloud/models/nextcloud.folder.model";

type FolderPickerState = "normal" | "loading" | "loaded" | "empty";

export interface FolderPickerScope {
  // Tree properties
  treeProps: FolderTreeProps;
  nextcloudTreeProps: FolderTreeProps;
  folderProps: FolderPickerProps;

  // Tree data
  trees: models.Tree[];
  nextcloudTrees: SyncDocument[];

  // UI state
  state: FolderPickerState;
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
  ($timeout) => {
    return {
      restrict: "E",
      scope: {
        folderProps: "=",
      },
      template: `
        <div class="horizontal-spacing-twice">
             <h2 translate content="[[folderProps.i18.title]]"></h2>
             <div ng-if="isStateNormal()">
                 <div class="row" ng-if="folderProps.i18.info">
                     <div class="info" translate content="[[folderProps.i18.info]]"></div>
                 </div>
                 <div class="row top-spacing-twice">
                     <div class="dual-tree-container">
                         <!-- Workspace tree -->
                         <div class="tree-container workspace-tree">
                             <h3><i18n>workspace.header</i18n></h3>
                             <folder-tree-2 tree-props="treeProps"></folder-tree-2>
                         </div>

                         <!-- Nextcloud tree -->
                         <div class="tree-container nextcloud-tree">
                             <h3><i18n>nextcloud.documents</i18n></h3>
                             <folder-tree-2 tree-props="nextcloudTreeProps"></folder-tree-2>
                         </div>
                     </div>
                     <hr />
                 </div>
                 <div class="lightbox-buttons fluid">
                     <button class="right-magnet" ng-disabled="cannotSubmit()" ng-click="onSubmit()" translate content="[[folderProps.i18.actionTitle]]"></button>
                     <button class="cancel right-magnet" ng-click="onCancel()"><i18n>cancel</i18n></button>
                 </div>
             </div>
        </div>
      `,
      link: async (scope: FolderPickerScope, element, attributes) => {
        // Initialize state
        scope.state = "normal";
        scope.search = { value: "" };
        scope.selectedFolder = null;
        scope.newFolder = new models.Element();

        // Safe apply helper
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

        // Add selectedFolder property to the scope
        scope.selectedFolder = null;

        // Initialize trees
        scope.trees = [];
        scope.nextcloudTrees = [];

        // Setup workspace tree props with direct object assignment to avoid reference issues
        scope.treeProps = {
          cssTree: "icons-view",
          trees: scope.trees, // This reference will be updated directly later
          isDisabled: (folder) => false,
          isSelectedFolder: (folder) => folder === scope.selectedFolder,
          isOpenedFolder: (folder) => {
            if (folder instanceof models.ElementTree) {
              return true;
            }
            // if (
            //   scope.selectedFolder &&
            //   scope.selectedFolder instanceof models.Element
            // ) {
            //   let parent = scope.selectedFolder;
            //   while (parent && parent.eParent) {
            //     if (parent === folder) {
            //       return true;
            //     }
            //     parent = parent.eParent;
            //   }
            // }
            return false;
          },
          openFolder: (folder) => {
            if (folder instanceof models.Element) {
              // Clear selection in the other tree
              if (scope.selectedFolder instanceof SyncDocument) {
                scope.selectedFolder = null;
              }
              scope.selectedFolder = folder;

              // Add selected-tree class to workspace tree
              element.find(".workspace-tree").addClass("selected-tree");
              element.find(".nextcloud-tree").removeClass("selected-tree");

              scope.safeApply();
            }
          },
        };

        // Setup Nextcloud tree props
        scope.nextcloudTreeProps = {
          cssTree: "icons-view",
          trees: scope.nextcloudTrees, // This reference will be updated directly later
          isDisabled: (folder) => false,
          isSelectedFolder: (folder) => folder === scope.selectedFolder,
          isOpenedFolder: (folder) => {
            if (
              scope.selectedFolder &&
              scope.selectedFolder instanceof SyncDocument
            ) {
              // For SyncDocument we need to check if it's in the path
              if (folder instanceof SyncDocument && folder.path) {
                // Root nextcloud folder
                if (
                  folder.isNextcloudParent &&
                  scope.selectedFolder instanceof SyncDocument
                ) {
                  return true;
                }

                // Check if the selected folder is inside this folder
                if (
                  scope.selectedFolder.path &&
                  scope.selectedFolder.path.startsWith(folder.path + "/")
                ) {
                  return true;
                }
              }
            }
            return false;
          },
          openFolder: (folder) => {
            if (folder instanceof SyncDocument) {
              // Clear selection in the other tree
              if (scope.selectedFolder instanceof models.Element) {
                scope.selectedFolder = null;
              }
              scope.selectedFolder = folder;

              // Add selected-tree class to nextcloud tree
              element.find(".nextcloud-tree").addClass("selected-tree");
              element.find(".workspace-tree").removeClass("selected-tree");

              scope.safeApply();
            }
          },
        };

        // Load trees
        const loadTrees = async () => {
          try {
            // Load workspace trees
            if (scope.folderProps.treeProvider) {
              const workspaceTrees = await scope.folderProps.treeProvider();
              // Update the array in place instead of reassigning
              if (workspaceTrees && workspaceTrees.length > 0) {
                scope.trees.length = 0; // Clear existing array
                workspaceTrees.forEach((tree) => scope.trees.push(tree)); // Add new items
              }
            }

            // Load Nextcloud trees
            if (scope.folderProps.nextcloudTreeProvider) {
              const nextcloudTrees =
                await scope.folderProps.nextcloudTreeProvider();
              // Update the array in place instead of reassigning
              if (nextcloudTrees && nextcloudTrees.length > 0) {
                scope.nextcloudTrees.length = 0; // Clear existing array
                nextcloudTrees.forEach((tree) =>
                  scope.nextcloudTrees.push(tree),
                ); // Add new items
              }
            }

            // Force digest cycle to update the view
            scope.safeApply();

            // Debug output to verify trees are populated
          } catch (e) {
            console.error("Error loading trees:", e);
            if (scope.folderProps.onError) {
              scope.folderProps.onError(e);
            }
          }
        };

        // Load trees
        await loadTrees();

        // State checking methods
        scope.isStateNormal = () => scope.state === "normal";
        scope.isStateLoading = () => scope.state === "loading";
        scope.isStateLoaded = () => scope.state === "loaded";
        scope.isStateEmpty = () => scope.state === "empty";

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
            // Use the submit function if provided
            if (scope.folderProps.submit) {
              scope.state = "loading";
              await scope.folderProps.submit(scope.selectedFolder);
              scope.state = "loaded";
              scope.safeApply();
            }
          } catch (e) {
            if (scope.folderProps.onError) {
              scope.folderProps.onError(e);
            }
            scope.state = "normal";
            scope.safeApply();
          }
        };

        // Cancel handler
        scope.onCancel = () => {
          if (scope.folderProps.onCancel) {
            scope.folderProps.onCancel();
          }
        };
      },
    };
  },
]);
