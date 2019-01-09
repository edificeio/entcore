import { angular, template } from "entcore";
import { models, workspaceService, WorkspacePreferenceView } from "../services";

export interface KeyboardDelegateScope {
    onSelectItem(e: Event, el: models.Element)
    onKeydown(event: KeyboardEvent)
    //from others
    selectedItems(): models.Element[];
    openCopyView()
    openMoveView()
    safeApply(a?);
    openedFolder: models.FolderContext
    isViewMode(mode: WorkspacePreferenceView): boolean
}

export function KeyboardDelegate($scope: KeyboardDelegateScope) {
    //
    let previous = null;
    $scope.onSelectItem = function (e: MouseEvent, el) {
        if (e && e.shiftKey && previous) {
            const all = $scope.isViewMode("list") ? $scope.openedFolder.sortedAll : $scope.openedFolder.all;
            let begin = all.findIndex(f => workspaceService.elementEqualsByRefOrId(f, previous));
            let end = all.findIndex(f => workspaceService.elementEqualsByRefOrId(f, el));
            //swap
            if (end < begin) {
                let temp = begin;
                begin = end;
                end = temp;
            }
            //
            if (0 <= begin && end < all.length) {
                for (let i = begin; i <= end; i++) {
                    all[i].selected = true;
                }
            }
        } else {
            previous = el;
        }
    }
    angular.element(document).bind('keydown', function (e) {
        $scope.onKeydown(e)
    });
    $scope.onKeydown = function (e) {
        if (!template.isEmpty("lightbox")) {
            return;
        }
        if (e.ctrlKey || e.metaKey) {
            if ((e.key == "a" || e.key == "A")) {
                const all = $scope.openedFolder.all;
                const selected = all.filter(a => a.selected);
                //toggle all selected
                const value = all.length == selected.length ? false : true;
                for (let a of all) {
                    a.selected = value;
                }
                e.preventDefault()
                $scope.safeApply()
            } else if ((e.key == "c" || e.key == "C")) {
                if ($scope.selectedItems().length) {
                    $scope.openCopyView();
                }
            } else if ((e.key == "x" || e.key == "X")) {
                if ($scope.selectedItems().length) {
                    $scope.openMoveView();
                }
            }
        }
    }
}