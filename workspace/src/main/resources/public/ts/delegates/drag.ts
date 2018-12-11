import { template, notify } from "entcore";
import { models, workspaceService } from "../services";

export interface DragDelegateScope {
    //from others
    trees: models.Tree[]
    currentTree: models.Tree
    isDraggingElement: boolean
    openedFolder: models.FolderContext
    onInit(cab: () => void);
    safeApply()
    selectedItems(): models.Element[];
    setMovingElements(elts: models.Element[])
    moveSubmit(dest: models.Element, elts?: models.Element[])
    //
    //drag and drop
    countDragItems(): number
    lockDropzone: boolean
    isDropzoneEnabled(): boolean
    canDropOnElement(el: models.Element): boolean
    dropMove(origin: models.Element[], target: models.Element)
    drag(item: models.Element, event?: any)
    dragEnd(item: models.Element, event?: any)
    dragCondition(item: models.Element)
    dropTrash(item: models.Element[])
    dropCondition(item: models.Element): (event) => boolean
    dropTo(item: models.Element, event?: any)
    //
}
declare var jQuery;
export function DragDelegate($scope: DragDelegateScope) {
    let draggingItems: models.Element[] = []
    $scope.onInit(function () {
        //INIT
        $scope.isDraggingElement = false;
    });
    $scope.lockDropzone = false;
    $scope.isDropzoneEnabled = function () {
        //display drop zone only owner and shared tree
        if ($scope.currentTree.filter == "owner" || $scope.currentTree.filter == "shared") {
            return !$scope.lockDropzone;
        } else {//lock
            return false;
        }
    }
    $scope.countDragItems = function () {
        return draggingItems.length;
    }
    $scope.dragEnd = function (el, event: Event) {
        //wait until drop event finished
        setTimeout(() => {
            //reset
            $scope.lockDropzone = false;
            $scope.isDraggingElement = false;
            $scope.safeApply()
        }, 300)
    }
    $scope.dropMove = async function (origin, target) {
        template.close('lightbox');
        if ($scope.currentTree.filter == "shared") {
            await workspaceService.moveAllForShared(origin, target)
        } else {
            await workspaceService.moveAll(origin, target)
        }
    };

    $scope.drag = function (item, $originalEvent: DragEvent) {
        //clean null values and keep unique values
        draggingItems = [...$scope.selectedItems(), item].filter(item => !!item).filter((elem, pos, arr) => {
            return arr.indexOf(elem) == pos;
        });
        $scope.lockDropzone = true;
        try {
            const original = jQuery($originalEvent.target);
            const copy = original.clone().css({ position: "absolute", top: -10000, left: -1000, zoom: 0.5 }).addClass("zoomout-2x").insertAfter(original)
            $originalEvent.dataTransfer.setDragImage(copy[0], undefined, undefined);
        } catch (e) {
            console.warn(e)
        }
        //
        try {
            $originalEvent.dataTransfer.setData('application/json', JSON.stringify(item));
        } catch (e) {
            $originalEvent.dataTransfer.setData('Text', JSON.stringify(item));
        }
        //
        $scope.isDraggingElement = true;
        $scope.safeApply()
    };

    $scope.dragCondition = function (item) {
        return $scope.currentTree.filter == "owner" || $scope.currentTree.filter == "protected" || ($scope.currentTree.filter == "shared" && item.canMove);
    }

    $scope.dropCondition = function (targetItem) {
        return function (event): boolean {
            if (!targetItem) {
                return false;
            }
            if (draggingItems.length == 0)
                return false
            //cannot drag on shared or protected tree
            const tree = (targetItem as models.Tree);
            const isTree = !!tree.filter;
            if (isTree && (tree.filter === 'shared' || tree.filter === 'protected'))
                return false
            //get real folder
            if (!isTree) {
                targetItem = workspaceService.findFolderInTrees($scope.trees, targetItem._id);
            }
            //check after search
            if (!targetItem) {
                return false;
            }
            //cannot drag on himself
            const targetIndex = draggingItems.filter(item => item === targetItem || (item && targetItem && item._id == targetItem._id))
            if (targetIndex.length > 0) {
                return false;
            }
            //can drop only on folder
            if (!isTree && targetItem.eType != models.FOLDER_TYPE) {
                return false;
            }
            //can drop on shared
            const fileIds = draggingItems.map(item => item._id);
            if (!isTree && targetItem.isShared && !targetItem.canCopyFileIdsInto(fileIds)) {
                return false;
            }
            //else accept
            return true
        }
    }
    $scope.canDropOnElement = function (el) {
        return $scope.isDraggingElement && $scope.dropCondition(el)(null);
    }

    $scope.dropTo = function (targetItem, $originalEvent) {
        const can = $scope.dropCondition(targetItem)($originalEvent);
        if (!can)
            return;
        //if drop on trash => trash
        if ((targetItem as models.Tree).filter === 'trash') {
            $scope.dropTrash(draggingItems);
        } else {
            //if drop from apps=> copy
            if ($scope.currentTree.filter === 'protected') {
                workspaceService.copyAll(draggingItems, targetItem)
            } else {
                //else use classic workflow
                $scope.moveSubmit(targetItem as models.Element, draggingItems)
            }
        }
        draggingItems = [];
    };

    $scope.dropTrash = async function (item) {
        const res = workspaceService.trashAll(item);
        notify.info('workspace.removed.message');
        await res;
    };
}

