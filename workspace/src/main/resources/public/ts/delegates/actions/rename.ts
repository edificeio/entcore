import { notify, template } from "entcore";
import { models, workspaceService } from "../../services";
import { ElementWithVisible } from "entcore/types/src/ts/workspace/services";

const RENAME_RIGHTS = ["org-entcore-workspace-controllers-WorkspaceController|renameDocument","org-entcore-workspace-controllers-WorkspaceController|renameFolder"]

export interface RenameDelegateScope {
    //from others
    currentTree: models.Tree
    selectedFolders(): models.Element[]
    selectedDocuments(): models.Element[]
    selectedItems(): models.Element[]
    //
    renameManagers:Array<{id:string, name:string, uri:string}>
    renameParent: ElementWithVisible;
    renameTarget: models.Element
    canRenameFolder(): boolean
    canRenameDocument(): boolean
    openRenameView(document: models.Element)
    rename(item: models.Element, newName: string)
    getRenameManagers():Array<{id:string, name:string}>
}

export function ActionRenameDelegate($scope: RenameDelegateScope) {
    $scope.renameManagers=[];
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

    $scope.openRenameView = async function () {
        $scope.renameManagers=[];
        try {
            const document = $scope.selectedItems()[0];
            const ownerId = typeof document.owner == "string"? document.owner: document.owner.userId;
            if (document.eParent) $scope.renameParent = await workspaceService.fetchParentInfo(document._id);
            else $scope.renameParent = undefined;
            //fetch managers
            $scope.renameManagers.push({id:ownerId, name:document.ownerName, uri:`/userbook/annuaire#/${ownerId}`});
            if($scope.renameParent && $scope.renameParent.inheritedShares){
                for(const share of $scope.renameParent.inheritedShares){
                    let addToManager = false;
                    for(const right of RENAME_RIGHTS){
                        if(share[right]){
                            addToManager = true;
                        }
                    }
                    if(addToManager){
                        const isUser = !!share.userId;
                        if(isUser){
                            if(share.userId != ownerId){
                                const found = $scope.renameParent.visibleUsers.find(u=>u.id==share.userId)||{} as any;
                                $scope.renameManagers.push({id:share.userId, name:found.username, uri:`/userbook/annuaire#${found.id}`});
                            }
                        } else {
                            const found = $scope.renameParent.visibleGroups.find(u=>u.id==share.groupId)||{} as any;
                            $scope.renameManagers.push({id:share.groupId, name:found.name, uri:`/userbook/annuaire#/group-view/${found.id}`});
                        }
                    }
                }
            }
            document.newName = document.newProperties ? document.newProperties.name : document.name;
            $scope.renameTarget = document;
            template.open('lightbox', 'rename');
        } catch(e){
            e.error && notify.error(e.error);
        }
    };

    $scope.rename = async function (item, newName) {
        template.close('lightbox');
        await workspaceService.rename(item, newName);
    }

    $scope.getRenameManagers = () => {
        return $scope.renameManagers;
    }
}