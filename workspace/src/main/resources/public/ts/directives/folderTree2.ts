/*
 * ⚠️ WARNING ⚠️
 * This component is almost an exact copy of the folderTree component
 * with a few adaptations to make it work with nextcloud.
 */

import { model, ng } from "entcore";
import { models, workspaceService } from "../services";
import angular = require("angular");
import { SyncDocument } from "./nextcloud/models/nextcloudFolder.model";
import { nextcloudService } from "./nextcloud/services/nextcloud.service";
import { GoogleDriveDocument } from "./google-drive/models/googleDriveDocument.model";
import { googleDriveService } from "./google-drive/services/googleDrive.service";

//function to compile template recursively
function compileRecursive($compile, element, link) {
  // Normalize the link parameter
  if (angular.isFunction(link)) {
    link = { post: link };
  }
  // Break the recursion loop by removing the contents
  const contents = element.contents().remove();
  let compiledContents;
  return {
    pre: link && link.pre ? link.pre : null,
    /**
     * Compiles and re-adds the contents
     */
    post: function (scope, element) {
      // Compile the contents
      if (!compiledContents) {
        compiledContents = $compile(contents);
      }
      // Re-add the compiled contents to the element
      compiledContents(scope, function (clone) {
        element.append(clone);
      });

      // Call the post-linking function, if any
      if (link && link.post) {
        link.post.apply(null, arguments);
      }
    },
  };
}
export interface FolderTreeProps<T = any> {
  cssTree?: string;
  trees: T[];
  isDisabled(folder: T): boolean;
  isSelectedFolder(folder: T): boolean;
  isOpenedFolder(folder: T): boolean;
  openFolder(folder: T): void;
}

export interface FolderTreeInnerScope<T = any> {
  folder: T;
  treeProps: FolderTreeProps<T>;
  translate();
  canExpendTree();
  isSelectedFolder(): boolean;
  isOpenedFolder(): boolean;
  openFolder();
  safeApply(a?: any);
  isDisabled(): boolean;
  isPersonalSpaceRoot(): boolean;
  isGoogleDriveRoot(): boolean;
  isNextcloudRoot(): boolean;
  isRegularFolder(): boolean;
}

export interface FolderTreeScope<T = any> {
  treeProps: FolderTreeProps<T>;
  trees(): T[];
}

export const folderTreeInner2 = ng.directive("folderTreeInner2", [
  "$compile",
  ($compile) => {
    return {
      restrict: "E",
      scope: {
        treeProps: "=",
        folder: "=",
      },
      template: `
        <a ng-class="{ selected: isSelectedFolder(), opened: isOpenedFolder(),'disabled-color':isDisabled() }" ng-click="openFolder()" ng-if="folder.name !== undefined"
        class="folder-list-item">
         <i class="arrow" ng-if="canExpendTree()" ng-class="{'disabled-color':isDisabled() }"></i>
         <span ng-if="isPersonalSpaceRoot()" style="display:inline-block;width:16px;height:16px;margin-right:6px;vertical-align:middle;color:#8c939e;">
           <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="16" height="16"><path fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" d="M3 11.5 12 4l9 7.5M5.5 10v9a1 1 0 0 0 1 1h3.5v-6h4v6H17a1 1 0 0 0 1-1v-9"/></svg>
         </span>
         <span ng-if="isGoogleDriveRoot()" style="display:inline-block;width:16px;height:14px;margin-right:6px;vertical-align:middle;">
           <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 87.3 78" width="16" height="14">
             <path fill="#0066da" d="m6.6 66.85 3.85 6.65c.8 1.4 1.95 2.5 3.3 3.3l13.75-23.8h-27.5c0 1.55.4 3.1 1.2 4.5z"/>
             <path fill="#00ac47" d="m43.65 25-13.75-23.8c-1.35.8-2.5 1.9-3.3 3.3l-25.4 44a9.06 9.06 0 0 0 -1.2 4.5h27.5z"/>
             <path fill="#ea4335" d="m73.55 76.8c1.35-.8 2.5-1.9 3.3-3.3l1.6-2.75 7.65-13.25c.8-1.4 1.2-2.95 1.2-4.5h-27.5l5.85 11.5z"/>
             <path fill="#00832d" d="m43.65 25 13.75-23.8c-1.35-.8-2.9-1.2-4.5-1.2h-18.5c-1.6 0-3.15.45-4.5 1.2z"/>
             <path fill="#2684fc" d="m59.8 53h-32.3l-13.75 23.8c1.35.8 2.9 1.2 4.5 1.2h50.8c1.6 0 3.15-.45 4.5-1.2z"/>
             <path fill="#ffba00" d="m73.4 26.5-12.7-22c-.8-1.4-1.95-2.5-3.3-3.3l-13.75 23.8 16.15 28h27.45c0-1.55-.4-3.1-1.2-4.5z"/>
           </svg>
         </span>
         <span ng-if="isNextcloudRoot()" style="display:inline-block;width:16px;height:16px;margin-right:6px;vertical-align:middle;">
           <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 128" width="16" height="16"><path fill="#0082c9" d="m128 7c-25.871 0-47.817 17.485-54.713 41.209-5.9795-12.461-18.642-21.209-33.287-21.209-20.304 0-37 16.696-37 37s16.696 37 37 37c14.645 0 27.308-8.7481 33.287-21.209 6.8957 23.724 28.842 41.209 54.713 41.209s47.817-17.485 54.713-41.209c5.9795 12.461 18.642 21.209 33.287 21.209 20.304 0 37-16.696 37-37s-16.696-37-37-37c-14.645 0-27.308 8.7481-33.287 21.209-6.8957-23.724-28.842-41.209-54.713-41.209zm0 22c19.46 0 35 15.54 35 35s-15.54 35-35 35-35-15.54-35-35 15.54-35 35-35zm-88 20c8.4146 0 15 6.5854 15 15s-6.5854 15-15 15-15-6.5854-15-15 6.5854-15 15-15zm176 0c8.4146 0 15 6.5854 15 15s-6.5854 15-15 15-15-6.5854-15-15 6.5854-15 15-15z"/></svg>
         </span>
         <span ng-if="isRegularFolder() && !isOpenedFolder()" style="display:inline-block;width:16px;height:16px;margin-right:6px;vertical-align:middle;color:#8c939e;">
           <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="16" height="16"><path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
         </span>
         <span ng-if="isRegularFolder() && isOpenedFolder()" style="display:inline-block;width:16px;height:16px;margin-right:6px;vertical-align:middle;color:#8c939e;">
           <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="16" height="16"><path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M2 8V6a2 2 0 0 1 2-2h4.5l2 2H20a2 2 0 0 1 2 2"/><path fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" d="M2 8h19a1 1 0 0 1 .97 1.24l-1.5 6A2 2 0 0 1 18.53 17H4.5a2 2 0 0 1-1.94-1.51L1 9.5A1 1 0 0 1 2 8Z"/></svg>
         </span>
         [[translate()]] <i class="loading" ng-if="folder.isChildrenLoading"></i>
        </a>
        <ul data-ng-class="{ selected: isOpenedFolder(), closed: !isOpenedFolder() }" ng-if="isOpenedFolder()">
            <li data-ng-repeat="child in folder.children">
                <folder-tree-inner-2 folder="child" tree-props="treeProps"></folder-tree-inner-2>
            </li>
        </ul>`,
      compile: function (element) {
        // Use the compile function from the RecursionHelper,
        // And return the linking function(s) which it returns
        return compileRecursive(
          $compile,
          element,
          (scope: FolderTreeInnerScope) => {
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
            scope.canExpendTree = function () {
              if (scope.folder instanceof GoogleDriveDocument) {
                // No upfront count/flag; children are only known after openFolder() expands it.
                return scope.folder.isFolder;
              }
              if (scope.folder instanceof models.Element && workspaceService.isLazyMode()) {
                return (
                  scope.folder.children.length > 0 ||
                  (scope.folder as models.Element).cacheChildren.isEmpty
                );
              }
              return scope.folder.children.length > 0;
            };
            scope.isSelectedFolder = function () {
              return scope.treeProps.isSelectedFolder(scope.folder);
            };
            scope.isOpenedFolder = function () {
              return scope.treeProps.isOpenedFolder(scope.folder);
            };
            scope.openFolder = async function () {
              if (scope.folder instanceof GoogleDriveDocument) {
                if (
                  !scope.folder.children ||
                  scope.folder.children.length === 0
                ) {
                  const children = await googleDriveService.listDocument(
                    model.me.userId,
                    scope.folder.id,
                  );
                  (scope.folder as GoogleDriveDocument).children =
                    children.filter((child) => child.isFolder);
                  scope.safeApply();
                }
              } else if (scope.folder instanceof SyncDocument) {
                if (
                  !scope.folder.children ||
                  scope.folder.children.length === 0
                ) {
                  const children = await nextcloudService.listDocument(
                    model.me.userId,
                    scope.folder.path,
                  );

                  (scope.folder as SyncDocument).children = children.filter(
                    (child) =>
                      child.isFolder &&
                      child.path !== "/" &&
                      child.path !== scope.folder.path,
                  );

                  scope.safeApply();
                }
              } else if (scope.folder instanceof models.Element && workspaceService.isLazyMode()) {
                if (scope.folder instanceof models.ElementTree) {
                  const temp = scope.folder as models.ElementTree;
                  await workspaceService.fetchChildrenForRoot(
                    temp,
                    { filter: temp.filter, hierarchical: false },
                    null,
                    { onlyFolders: true },
                  );
                } else {
                  await workspaceService.fetchChildren(
                    scope.folder as models.Element,
                    { filter: "all", hierarchical: false },
                    null,
                    { onlyFolders: true },
                  );
                }
              }
              const ret = scope.treeProps.openFolder(scope.folder);
              scope.safeApply();
              return ret;
            };
            scope.translate = function () {
              return scope.folder.name;
            };
            scope.isDisabled = function () {
              return scope.treeProps.isDisabled(scope.folder);
            };
            scope.isPersonalSpaceRoot = function () {
              return !!(scope.folder as any).isPersonalSpaceWrapper;
            };
            scope.isGoogleDriveRoot = function () {
              return !!(scope.folder as any).isGoogleDriveRootWrapper;
            };
            scope.isNextcloudRoot = function () {
              return !!(scope.folder as any).isRootGroup;
            };
            scope.isRegularFolder = function () {
              return !scope.isPersonalSpaceRoot() && !scope.isGoogleDriveRoot() && !scope.isNextcloudRoot();
            };
          },
        );
      },
    };
  },
]);

export const folderTree2 = ng.directive("folderTree2", [
  "$templateCache",
  ($templateCache) => {
    return {
      restrict: "E",
      scope: {
        treeProps: "=",
      },
      template: `
       <nav class="vertical mobile-navigation" ng-class="treeProps.cssTree">
          <ul>
            <li data-ng-repeat="folder in trees()">
                <folder-tree-inner-2 folder="folder" tree-props="treeProps"></folder-tree-inner-2>
            </li>
          </ul>
        </nav>
        `,
      link: async (scope: FolderTreeScope) => {
        scope.trees = function () {
          return scope.treeProps ? scope.treeProps.trees : [];
        };
      },
    };
  },
]);
