import { template, SharePayload, ShareAction, model, notify, idiom } from "entcore";
import { models, workspaceService } from "../../services";


export interface ShareDelegateScope {
    copyingForShare: boolean;
    sharedElements: models.Element[]
    openShareView();
    canShareElements(): boolean
    onCancelShare()
    onShareAndCopy()
    onShareAndNotCopy()
    onCancelShareElements()
    onSubmitSharedElements(shared: SharePayload);
    canEditShareItem(args: { id: string, type: string })
    onValidateShare(data: SharePayload, resource: models.Element, actions: ShareAction[]): Promise<any>
    onShareFeed(data: any, resource: models.Element, actions: ShareAction[])
    //from others
    currentTree: models.Tree;
    display: { nbFiles: number, importFiles?: boolean, viewFile?: models.Element, share?: boolean }
    isSearchResult(): boolean;
    selectedItems(): models.Element[]
    setHighlighted(els: models.Element[])
    setCurrentTreeRoute(tree: models.TREE_NAME);
}

export function ActionShareDelegate($scope: ShareDelegateScope) {
    let copiedFolders: models.Element[] = []
    $scope.sharedElements = [];
    const needShareWarning = function () {
        if ($scope.isSearchResult()) {
            const foundedShareFolder = $scope.sharedElements.find(a => workspaceService.isFolder(a) && a.isShared);
            return !!foundedShareFolder;
        } else if ($scope.currentTree.filter != "shared") {
            const founded = $scope.sharedElements.find(a => workspaceService.isFolder(a));
            if (founded) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    $scope.openShareView = function () {
        $scope.copyingForShare = false;
        $scope.sharedElements = $scope.selectedItems();
        copiedFolders = [];
        $scope.display.share = true;
        $scope.canEditShareItem = function (args) {
            if (args.type == "user") {
                const uniqOwnerIds = $scope.sharedElements.map(sha => sha.owner.userId).filter((elem, pos, arr) => {
                    return arr.indexOf(elem) == pos;
                });
                //can edit only if the user is not the owner
                if (uniqOwnerIds.length == 1) {
                    return uniqOwnerIds[0] != args.id;
                }
            }
            return true;
        }
        if (needShareWarning()) {
            template.open('share', 'share/share-folders-warning');
        } else {
            template.open('share', 'share/share');
        }
    };
    const closeShareView = function () {
        template.close("share")
        $scope.display.share = false;
        copiedFolders = []
        $scope.sharedElements = []
    }
    const countShared = function (shared: SharePayload, excludedOwner: string = null) {
        let count = 0;
        if (!shared) {
            return count;
        }
        count += shared.users ? Object.keys(shared.users).length : 0;
        count += shared.groups ? Object.keys(shared.groups).length : 0;
        if (excludedOwner && shared.users && shared.users[excludedOwner]) {
            count--;
        }
        return count;
    }
    $scope.onShareFeed = function (data, resource, actions) {
        //sometimes owner is a string? 
        const userId: any = resource.owner.userId || resource.owner;
        //if owner is current user => skip
        if (userId == model.me.userId) {
            return;
        }
        //if owner is  in shared or inherithshared=>skip
        if (data.users["checked"][userId] || data.users["checkedInherited"][userId]) {
            return;
        }
        //get ownername
        const userName: any = resource.owner.displayName || resource.ownerName;
        //add owner as managaer => collect all actions
        const actionsNames = actions.map(a => a.name).reduce((prev, current) => prev.concat(current), [])
        if (!actionsNames.length) {
            throw "could not found actions"
        }
        //add owner as managaer=>push user visible and checked
        data.users["checked"][userId] = actionsNames;
        data.users["visibles"].push({
            id: userId,
            username: userName,
            type: "user"
        })
    }
    $scope.onValidateShare = async function (data, resource, actions) {
        //owner is always manager of his folder
        if (workspaceService.isFolder(resource)) {
            //sometimes owner is a string? 
            const userId: any = resource.owner.userId || resource.owner;
            const count = countShared(data, userId);
            if (count <= 0) {
                //if no shared => remove owner from shared
                delete data.users[userId];
                return true;
            }
            //if there are some shared=> add owner as managaer
            const actionsNames = actions.map(a => a.name).reduce((prev, current) => prev.concat(current), [])
            if (!actionsNames.length) {
                throw "could not found actions"
            }
            data.users[userId] = actionsNames;
            return true;
        } else {
            return true;
        }
    }
    $scope.canShareElements = function () {
        return $scope.selectedItems().length > 0 && ($scope.currentTree.filter == "shared" || $scope.currentTree.filter == "owner")
    };
    $scope.onCancelShare = function () {
        closeShareView()
    }
    $scope.onShareAndCopy = async function () {
        const folders = $scope.sharedElements.filter(a => workspaceService.isFolder(a));
        try {
            $scope.copyingForShare = true;
            const res = await workspaceService.copyAll(folders, $scope.currentTree as models.Element)
            copiedFolders = res.copies;
            template.open('share', 'share/share');
        } finally {
            $scope.copyingForShare = false;
        }
    }
    $scope.onShareAndNotCopy = function () {
        template.open('share', 'share/share');
    }
    $scope.onCancelShareElements = async function () {
        if (copiedFolders.length) {
            await workspaceService.deleteAll(copiedFolders)
        }
        closeShareView()
    }
    $scope.onSubmitSharedElements = function (shared) {
        const count = countShared(shared);
        if (count <= 0) {
            notify.success(idiom.translate("workspace.share.removeall"))
        }
        //
        $scope.sharedElements && $scope.sharedElements.forEach(el => {
            el._isShared = true;
        })
        workspaceService.onChange.next({ action: "tree-change", treeSource: $scope.currentTree.filter, treeDest: "shared", elements: $scope.sharedElements });
        //DONT NEED
        //$scope.setCurrentTreeRoute("shared")
        closeShareView()
        setTimeout(() => {
            // hightlight by ids?
            //$scope.setHighlighted(copiedFolders);
        })
    }
}