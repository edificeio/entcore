import { template, notify, quota } from "entcore";
import { models, workspaceService } from "../../services";


export interface ActionTrashScope {

    //delete
    restore();
    deleteConfirm()
    confirm()
    toTrash()
    toTrashConfirm()
    deleteSelection();
    //
    emptyTrash();
    canEmptyTrash(): boolean;
    //from others
    currentTree: models.Tree
    setCurrentTreeRoute(tree: models.TREE_NAME, forceReload?: boolean);
    safeApply()
    selectedItems(): models.Element[]
}


export function ActionTrashDelegate($scope: ActionTrashScope) {
    /**
     * Trash
     */
    $scope.toTrashConfirm = function () {
        if ($scope.currentTree.filter == "shared") {
            template.open('lightbox', 'trash/confirm-shared');
        } else if ($scope.currentTree.filter == "protected") {
            template.open('lightbox', 'trash/confirm');
        } else {
            //owner
            $scope.toTrash();
        }
        $scope.confirm = async function () {
            const all = $scope.selectedItems()
            await workspaceService.trashAll(all);
            notify.info('workspace.removed.message');
            template.close('lightbox');
        };
    };

    $scope.toTrash = async function () {
        const removed = $scope.selectedItems()
        await workspaceService.trashAll(removed);
        notify.info('workspace.removed.message');
        template.close('lightbox');
    };

    $scope.restore = async function () {
        const all = $scope.selectedItems()
        await workspaceService.restoreAll(all)
        notify.info('workspace.restored.message');
    };

    /**
  * Delete
  */
    $scope.canEmptyTrash = function () {
        return $scope.currentTree.filter === 'trash';
    }

    $scope.emptyTrash = function () {
        //dont be in a folder when deleting
        $scope.setCurrentTreeRoute("trash")
        workspaceService.emptyTrash();
    };

    $scope.deleteSelection = async function () {
        const all = $scope.selectedItems()
        const res = await workspaceService.deleteAll(all)
        if (res.nbFiles) {
            await quota.refresh();
            $scope.safeApply();
        }
    };
    $scope.deleteConfirm = function () {
        template.open('lightbox', 'trash/confirm-delete');
        $scope.confirm = async function () {
            const all = $scope.selectedItems()
            await workspaceService.deleteAll(all);
            notify.info('workspace.deleted.message');
            template.close('lightbox');
        };
    };
}