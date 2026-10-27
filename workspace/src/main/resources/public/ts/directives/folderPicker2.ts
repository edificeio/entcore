/*
 * ⚠️ WARNING ⚠️
 * This component is almost an exact copy of the folderPicker component
 * with a few adaptations to make it work with nextcloud.
 */

import { ng } from "entcore";
import { models, workspaceService } from "../services";
import { FolderTreeProps } from "./folderTree2";
import { SyncDocument } from "./nextcloud/models/nextcloudFolder.model";
import { GoogleDriveDocument } from "./google-drive/models/googleDriveDocument.model";

export interface FolderPickerScope {
  treeProps: FolderTreeProps<models.Tree>;
  nextcloudTreeProps: FolderTreeProps<SyncDocument>;
  googleDriveTreeProps: FolderTreeProps<GoogleDriveDocument>;
  folderProps: FolderPickerProps;

  // Tree data
  trees: models.Tree[];
  nextcloudTrees: SyncDocument[];
  googleDriveTrees: GoogleDriveDocument[];

  // UI state
  selectedFolder: models.Element | SyncDocument | GoogleDriveDocument;
  isSubmitting: boolean;
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
  // Id of the folder the file currently lives in — null for a drive's root folder (e.g.
  // GoogleDriveDocument.initParent()), distinct from undefined ("not set" — the picker behaves exactly
  // as before). Only set by callers that want the picker to refuse selecting that same folder back as
  // the destination (e.g. a "move" source — a no-op at best, and for some drives an outright error).
  parentId?: string | null;
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
  googleDriveTreeProvider?(): Promise<GoogleDriveDocument[]>;
  manageSubmit?(folder: models.Element | SyncDocument | GoogleDriveDocument): boolean;
  submit?(folder: models.Element | SyncDocument | GoogleDriveDocument): Promise<void> | void;
  onCancel(): void;
  onError?(error: any): void;
  onSubmitSuccess?(dest: models.Element | SyncDocument | GoogleDriveDocument, count: number): void;
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
              <!-- Google Drive tree -->
              <div class="tree-container google-drive-tree" ng-if="folderProps.googleDriveTreeProvider">
                <folder-tree-2 tree-props="googleDriveTreeProps"></folder-tree-2>
              </div>
              <hr />
              <div class="lightbox-buttons fluid">
                  <!-- Plain right-magnet/cancel (no "nextcloud-"-prefixed classes): this picker is shared
                       by the classic space, Google Drive and Nextcloud alike, but those prefixed classes
                       carry Nextcloud's own hardcoded app branding (panda theme CSS scopes them under
                       .sniplet.nextcloud .nextcloud-content-toolbar) — so a dialog opened from Nextcloud
                       picked up a different, Nextcloud-colored button style than the identical dialog
                       opened from "Mon espace personnel", which this component must look the same from. -->
                  <button class="right-magnet" ng-disabled="cannotSubmit()" ng-click="onSubmit()">
                    <i class="google-drive-import-spinner" ng-if="isSubmitting" style="margin-right: 6px; vertical-align: middle;"></i>
                    <span ng-if="!isSubmitting" translate content="[[folderProps.i18.actionTitle]]"></span>
                    <span ng-if="isSubmitting" translate content="[[folderProps.i18.actionProcessing]]"></span>
                  </button>
                  <button class="cancel right-magnet" ng-disabled="isSubmitting" ng-click="onCancel()"><i18n>cancel</i18n></button>
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
        // Defined here, before the async loadTrees() below, so ng-disabled="cannotSubmit()" never
        // evaluates against an undefined function on the directive's first render — otherwise the
        // button renders enabled until some unrelated event (e.g. the user's own click) forces a
        // fresh digest that finally re-evaluates it correctly.
        scope.isSubmitting = false;
        scope.cannotSubmit = () => !scope.selectedFolder || scope.isSubmitting;

        scope.trees = [];
        scope.nextcloudTrees = [];
        scope.googleDriveTrees = [];

        // Folders a "move" source is currently in: refused as a destination (see FolderPickerSourceFile.
        // parentId) — moving a file back into its own folder is a no-op at best. Only populated when a
        // caller actually sets parentId on its move sources; other callers (copy/duplicate, or move
        // sources that don't set it) are unaffected.
        const excludedFolderIds = new Set<string | null>(
          (scope.folderProps.sources || [])
            .filter((source) => source.action === "move-from-file" && (source as any).parentId !== undefined)
            .map((source) => (source as any).parentId as string | null),
        );

        const canSelect = function (folder: models.Element | SyncDocument | GoogleDriveDocument) {
          // Synthetic label node (see setupCopyProps's ownerWrapper) grouping the owner trees under
          // "Mon espace personnel" — navigation/expand only, not a real folder to export/move into.
          if ((folder as any).isPersonalSpaceWrapper) {
            return false;
          }
          if (excludedFolderIds.has((folder as any).id)) {
            return false;
          }
          if (folder instanceof models.Element) {
            if ((folder as models.Tree).filter) {
              return (folder as models.Tree).filter == "owner";
            } else {
              return true;
            }
          } else if (folder instanceof SyncDocument) {
            // isRootGroup: synthetic "Nextcloud" label node wrapping "Mes documents"/"Corbeille"
            // (SyncDocument.createRootGroup()) — same navigation-only treatment as isPersonalSpaceWrapper.
            return folder.isFolder && !(folder as any).isRootGroup;
          } else if (folder instanceof GoogleDriveDocument) {
            // isGoogleDriveRootWrapper: synthetic "Google Drive" label node wrapping "Mes documents"
            // (set in setupCopyProps's googleDriveTreeProvider) — same treatment.
            return folder.isFolder && !(folder as any).isGoogleDriveRootWrapper;
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
            selectedGoogleDriveFolder = null;
            openedGoogleDriveFolders = [];

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
            return !canSelect(folder);
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
            // Clear workspace and Google Drive selection
            selectedWorkspaceFolder = null;
            openedWorkspaceFolder = null;
            selectedGoogleDriveFolder = null;
            openedGoogleDriveFolders = [];

            if (canSelect(folder)) {
              selectedNextcloudFolder = folder;
              scope.selectedFolder = folder;
            }

            // Add to opened folders if not already there
            if (!openedNextcloudFolders.includes(folder)) {
              openedNextcloudFolders.push(folder);
            }
          },
        };

        // Google Drive tree props
        let selectedGoogleDriveFolder: GoogleDriveDocument = null;
        let openedGoogleDriveFolders: GoogleDriveDocument[] = [];

        scope.googleDriveTreeProps = {
          cssTree: "maxheight-half-vh",
          get trees() {
            return scope.googleDriveTrees;
          },
          isDisabled(folder: GoogleDriveDocument) {
            return !canSelect(folder);
          },
          isOpenedFolder(folder: GoogleDriveDocument) {
            return openedGoogleDriveFolders.some(
              (openFolder) => openFolder === folder,
            );
          },
          isSelectedFolder(folder: GoogleDriveDocument) {
            return selectedGoogleDriveFolder === folder;
          },
          openFolder(folder: GoogleDriveDocument) {
            // Clear workspace and Nextcloud selection
            selectedWorkspaceFolder = null;
            openedWorkspaceFolder = null;
            selectedNextcloudFolder = null;
            openedNextcloudFolders = [];

            if (canSelect(folder)) {
              selectedGoogleDriveFolder = folder;
              scope.selectedFolder = folder;
            }

            if (!openedGoogleDriveFolders.includes(folder)) {
              openedGoogleDriveFolders.push(folder);
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

            // Load Google Drive trees
            if (scope.folderProps.googleDriveTreeProvider) {
              const googleDriveTrees =
                await scope.folderProps.googleDriveTreeProvider();
              if (googleDriveTrees && googleDriveTrees.length > 0) {
                scope.googleDriveTrees.length = 0;
                googleDriveTrees.forEach((tree) =>
                  scope.googleDriveTrees.push(tree),
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
              scope.isSubmitting = true;
              scope.safeApply();
              await scope.folderProps.submit(scope.selectedFolder);
              scope.isSubmitting = false;
              scope.safeApply();
            }
          } catch (e) {
            scope.isSubmitting = false;
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
