import { notify, template } from "entcore";
import { models, workspaceService } from "../../services";


export interface RenameDelegateScope {
    //from others
    currentTree: models.Tree
    selectedFolders(): models.Element[]
    selectedDocuments(): models.Element[]
    selectedItems(): models.Element[]
    //
    renameTarget: models.Element
    canRenameFolder(): boolean
    canRenameDocument(): boolean
    openRenameView(document: models.Element)
    rename(item: models.Element, newName: string)
}

export function ActionRenameDelegate($scope: RenameDelegateScope) {
    /**
      * Rename Action
      */
    $scope.canRenameDocument = function () {
        const totalFold = $scope.selectedFolders().length
        const totalDoc = $scope.selectedDocuments().length;
        return totalFold == 0 && totalDoc == 1 && $scope.currentTree.filter != "trash";
    }

    $scope.canRenameFolder = function () {
        const totalFold = $scope.selectedFolders().length
        const totalDoc = $scope.selectedDocuments().length;
        return totalFold == 1 && totalDoc == 0 && $scope.currentTree.filter != "trash";
    }

    $scope.openRenameView = function () {
        const document = $scope.selectedItems()[0];
        document.newName = document.newProperties ? document.newProperties.name : document.name;
        $scope.renameTarget = document;
        template.open('lightbox', 'rename');
    };

    $scope.rename = async function (item, newName) {
        template.close('lightbox');
        await workspaceService.rename(item, newName);
    }
}