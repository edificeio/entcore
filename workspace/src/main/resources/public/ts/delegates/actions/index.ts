import { models, workspaceService } from "../../services";
import { ActionRenameDelegate, RenameDelegateScope } from "./rename";
import { ActionCreateDelegate, CreateDelegateScope } from "./create";
import { ActionShareDelegate, ShareDelegateScope } from "./share";
import { ActionCopyDelegate, ActionCopyDelegateScope } from "./copy";
import { ActionTrashScope, ActionTrashDelegate } from "./trash";


export interface ActionDelegateScope extends RenameDelegateScope, CreateDelegateScope, ShareDelegateScope, ActionCopyDelegateScope, ActionTrashScope {
    //from others
    display: { nbFiles: number, importFiles?: boolean, editedImage?: models.Element, editImage?: boolean }
    currentTree: models.Tree
    openedFolder: models.FolderContext
    selectedFolders(): models.Element[]
    selectedDocuments(): models.Element[]
    selectedItems(): models.Element[]
    onInit(cab: () => void);
    //edit image
    canEditImage(): boolean
    editImage()
    //base
    canDownload();
    downloadFile()
    //
    safeApply()
}
export function ActionDelegate($scope: ActionDelegateScope) {
    $scope.onInit(function () {
        //INIT
    });
    ActionCreateDelegate($scope)
    ActionRenameDelegate($scope)
    ActionShareDelegate($scope);
    ActionCopyDelegate($scope)
    ActionTrashDelegate($scope)

    /**
     * Edit Image
     */
    $scope.canEditImage = function () {
        return $scope.selectedFolders().length === 0 && $scope.selectedDocuments().length == 1 && $scope.selectedDocuments()[0].isEditableImage && ($scope.currentTree.filter === 'shared' || $scope.currentTree.filter === 'owner' || $scope.currentTree.filter === 'protected');
    }
    $scope.editImage = function () {
        $scope.display.editImage = true;
        $scope.display.editedImage = $scope.selectedDocuments()[0];
    }
    /**
     * Download
     */
    $scope.canDownload = function () {
        //cant download external ressources
        if($scope.currentTree.filter=="external"){
            return false;
        }
        //
        const items = $scope.selectedItems();
        if(!workspaceService.isActionAvailable("download",items)){
            return false;
        }
        //
        return items.length > 0;
    }

    $scope.downloadFile = function () {
        workspaceService.downloadFiles($scope.selectedItems(),$scope.currentTree.filter=="trash")
    };
}

