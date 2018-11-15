import { template, notify } from "entcore";
import { models, workspaceService } from "../services";

export interface DragDelegateScope {
    //from others
    currentTree: models.Tree
    onInit(cab: () => void);
    safeApply()
    //
    //drag and drop
    lockDropzone: boolean
    dragMove(origin: models.Element, target: models.Element)
    drag(item: models.Element, event?: any)
    dragEnd(item: models.Element, event?: any)
    dragCondition(item: models.Element)
    dragToTrash(item: models.Element)
    dropCondition(item: models.Element)
    dropTo(item: models.Element, event?: any)
    dropTrash(item: models.Element)
    dropMove(source: models.Element, dest: models.Element)
    //
}
declare var jQuery;
export function DragDelegate($scope: DragDelegateScope) {
    $scope.onInit(function () {
        //INIT
    });
    $scope.lockDropzone = false;
    $scope.dragEnd = function (el, event: Event) {
        //reset
        $scope.lockDropzone = false;
        jQuery(event.target).removeClass("zoomout-2x")
        $scope.safeApply()
    }
    $scope.dragMove = async function (origin, target) {
        template.close('lightbox');
        if ($scope.currentTree.filter == "shared") {
            await workspaceService.moveAllForShared([origin], target)
        } else {
            await workspaceService.moveAll([origin], target)
        }
    };

    $scope.drag = function (item, $originalEvent: DragEvent) {
        jQuery($originalEvent.target).addClass("zoomout-2x")
        $scope.lockDropzone = true;
        try {
            $originalEvent.dataTransfer.setData('application/json', JSON.stringify(item));
        } catch (e) {
            $originalEvent.dataTransfer.setData('Text', JSON.stringify(item));
        }
    };

    $scope.dragCondition = function (item) {
        return $scope.currentTree.filter == "owner" || ($scope.currentTree.filter == "shared" && item.canMove);
    }

    $scope.dropCondition = function (targetItem) {
        return function (event) {
            const { types } = event.dataTransfer;
            const dataField = types.indexOf && types.indexOf("application/json") > -1 ? "application/json" : //Chrome & Safari
                types.contains && types.contains("application/json") ? "application/json" : //Firefox
                    types.contains && types.contains("Text") ? "Text" : //IE
                        undefined
            const tree = (targetItem as models.Tree);
            if (!dataField || tree.filter === 'shared' || tree.filter === 'protected')
                return false

            return dataField
        }
    }

    $scope.dropTo = function (targetItem, $originalEvent) {
        const dataField = $scope.dropCondition(targetItem)($originalEvent);
        const originalItem = JSON.parse($originalEvent.dataTransfer.getData(dataField));

        if (originalItem._id === targetItem._id)
            return;

        if ((targetItem as models.Tree).filter === 'trash')
            $scope.dropTrash(originalItem);
        else
            $scope.dropMove(originalItem, targetItem);
    };

    $scope.dropMove = function (originalItem, targetItem) {
        $scope.dragMove(originalItem, targetItem)
    }
    $scope.dropTrash = function (originalItem) {
        $scope.dragToTrash(originalItem)
    }

    $scope.dragToTrash = async function (item) {
        const res = workspaceService.trashAll([item]);
        notify.info('workspace.removed.message');
        await res;
    };
}

