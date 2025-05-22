/*
 * ⚠️ WARNING ⚠️
 * This component is almost an exact copy of the folderTree component
 * with a few adaptations to make it work with nextcloud.
 */

import { ng } from "entcore";
import { models, workspaceService } from "../services";
import angular = require("angular");

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
export interface FolderTreeProps {
  cssTree?: string;
  trees: any;
  isDisabled(folder: models.Element): boolean;
  isSelectedFolder(folder: models.Element): boolean;
  isOpenedFolder(folder: models.Element): boolean;
  openFolder(folder: models.Element);
}
export interface FolderTreeInnerScope {
  folder: models.Element;
  treeProps: FolderTreeProps;
  translate();
  canExpendTree();
  isSelectedFolder(): boolean;
  isOpenedFolder(): boolean;
  openFolder();
  safeApply(a?: any);
  isDisabled(): boolean;
}

export interface FolderTreeScope {
  treeProps: FolderTreeProps;
  trees(): models.Tree[];
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
         <i class="arrow" ng-if="canExpendTree()" ng-class="{'disabled-color':isDisabled() }"></i> [[translate()]] <i class="loading" ng-if="folder.isChildrenLoading"></i>
        </a>
        <ul data-ng-class="{ selected: isOpenedFolder(), closed: !isOpenedFolder() }" ng-if="isOpenedFolder()">
            <li data-ng-repeat="child in folder.children">
                <folder-tree-inner folder="child" tree-props="treeProps"></folder-tree-inner>
            </li>
        </ul>`,
      compile: function (element) {
        // Use the compile function from the RecursionHelper,
        // And return the linking function(s) which it returns
        return compileRecursive(
          $compile,
          element,
          (scope: FolderTreeInnerScope) => {
            console.log(scope.folder);
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
              if (workspaceService.isLazyMode()) {
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
              if (workspaceService.isLazyMode()) {
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
                        <folder-tree-inner folder="folder" tree-props="treeProps"></folder-tree-inner>
                    </li>
                </ul>
        </nav>
        `,
      link: async (scope: FolderTreeScope, element, attributes) => {
        scope.trees = function () {
          return scope.treeProps ? scope.treeProps.trees : [];
        };
      },
    };
  },
]);
