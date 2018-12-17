import { notify, template, idiom as lang } from "entcore";
import { models, workspaceService } from "../../services";


export interface CreateDelegateScope {
    //share
    newElementSharing: models.Element[];
    newFolder: models.Element
    onSubmitSharedNewFolder($event: any)
    onCloseShareNewFolder($canceled, $close);
    onCancelShareNewFolderDelete();
    onCancelShareNewFolderOwn();
    //
    showNewElementSharedInfo();
    canCreateNewFolder(): boolean;
    canCreateNewFolderShared(): boolean;
    openNewFolderView(): void;
    createFolder();
    isSharedTree(): boolean;
    onImportFiles(files: FileList)
    canDropOnFolder(): boolean
    onCannotDropFile();
    onCloseShareNewFiles($canceled, $close)
    onCancelShareNewFilesOwn();
    onSubmitSharedNewFiles($event: any)
    onCancelShareNewFilesDelete();
    //from others
    currentTree: models.Tree
    openedFolder: models.FolderContext
    setHighlighted(els: models.Element[])
    setCurrentTreeRoute(tree: models.TREE_NAME, forceReload?: boolean);
    setLightboxDelegateClose(f: () => boolean)
}

export function ActionCreateDelegate($scope: CreateDelegateScope) {
    $scope.newElementSharing = [];
    $scope.onCannotDropFile = function () {
        notify.error(lang.translate("workspace.contrib.cant"))
    }
    $scope.canDropOnFolder = function () {
        const folder = $scope.openedFolder.folder;
        const filter = (folder as models.Tree).filter;
        //must return a boolean
        if (filter) {
            return !!(filter === "owner" || filter === "shared");
        } else {
            return !!(folder._id && folder.canWriteOnFolder);
        }
    }
    /**
     * Share
     */
    const needAtLeastOneShared = function () {
        return $scope.currentTree.filter == "shared" && $scope.openedFolder.folder && !$scope.openedFolder.folder.isShared
    }
    $scope.onSubmitSharedNewFolder = function (event) {
        const founded = $scope.newElementSharing.map(e => {
            e._isShared = true;
            //restart animation
            let founded: models.Element = null;
            workspaceService.findFolderInTreeByRefOrId($scope.currentTree, e, (f) => {
                founded = f;
            })
            return founded;
        });
        $scope.setHighlighted(founded);
        $scope.newElementSharing = [];
    }
    $scope.onCloseShareNewFolder = function ($canceled, $close) {
        if ($canceled) {
            if (needAtLeastOneShared()) {
                template.open('lightbox', 'create-folder/shared-cancel');
                //apply delete action when cancelling window
                $scope.setLightboxDelegateClose(() => {
                    $scope.onCancelShareNewFolderDelete()
                    return false;
                })
            } else {
                template.close("lightbox")
            }
        } else {
            $close();
        }
    }
    $scope.onCancelShareNewFolderDelete = async function () {
        template.close("lightbox")
        await workspaceService.deleteAll($scope.newElementSharing)
        $scope.newElementSharing = [];
    }
    $scope.onCancelShareNewFolderOwn = async function () {
        workspaceService.onChange.next({ action: "delete", treeDest: "shared", elements: $scope.newElementSharing })
        $scope.setCurrentTreeRoute("owner")
        template.close("lightbox")
        const elements = $scope.newElementSharing;
        $scope.newElementSharing = [];
        //hightlight
        setTimeout(() => {
            workspaceService.onChange.next({ action: "add", treeDest: "owner", elements })
            $scope.setHighlighted(elements);
        }, 300)
    }
    $scope.showNewElementSharedInfo = function () {
        return needAtLeastOneShared();
    }
    /**
     * Create folder
     */
    $scope.canCreateNewFolder = function () {
        //cannot create normal folder on trash/apps/shared
        return $scope.currentTree.filter == "owner";
    }
    $scope.canCreateNewFolderShared = function () {
        const isSharedTree = $scope.currentTree.filter == "shared";
        //only on shared and only if i have contrib right
        const current = $scope.openedFolder.folder;
        if (current && current._id) {
            return isSharedTree && current.canWriteOnFolder;
        } else {
            return isSharedTree;
        }
    }
    $scope.isSharedTree = function () {
        return $scope.currentTree.filter == "shared";
    }
    $scope.createFolder = async function () {
        const res = await workspaceService.createFolder($scope.newFolder, $scope.openedFolder.folder)
        const error = (res as any).error;
        if (error) {
            notify.error(error);
        } else {
            const newFolder: models.Element = res as any;
            if ($scope.currentTree.filter == "shared" && needAtLeastOneShared()) {
                $scope.newElementSharing = [newFolder];
                template.open('lightbox', 'create-folder/shared-step2');
            } else {
                template.close('lightbox');
            }
            $scope.newFolder = models.emptySharedFolder()
        }
    };
    $scope.openNewFolderView = function () {
        $scope.newFolder = models.emptySharedFolder();
        if ($scope.currentTree.filter == "shared") {
            template.open('lightbox', 'create-folder/shared-step1');
        } else {
            template.open('lightbox', 'create-folder/simple');
        }
    };
    /**
     * Import file
     */
    const openNewFileView = function (els: models.Element[]) {
        if ($scope.currentTree.filter == "shared" && needAtLeastOneShared()) {
            $scope.newElementSharing = els;
            template.open('lightbox', 'import-file/shared-step');
        } else {
            $scope.newElementSharing = [];
        }
    }
    workspaceService.onConfirmImport.subscribe(ev => {
        setTimeout(() => {
            $scope.setHighlighted(ev);
        }, 300)
        openNewFileView(ev);
    })
    $scope.onImportFiles = function (files) {
        workspaceService.onImportFiles.next(files);
    }
    $scope.onCloseShareNewFiles = function ($canceled, $close) {
        if ($canceled) {
            if (needAtLeastOneShared()) {
                template.open('lightbox', 'import-file/shared-cancel');
                //apply delete action when cancelling window
                $scope.setLightboxDelegateClose(() => {
                    $scope.onCancelShareNewFilesDelete();
                    return false;
                })
            } else {
                template.close("lightbox")
            }
        } else {
            $close()
        }
    }
    $scope.onCancelShareNewFilesOwn = async function () {
        workspaceService.onChange.next({ action: "delete", treeDest: "shared", elements: $scope.newElementSharing })
        $scope.setCurrentTreeRoute("owner")
        template.close("lightbox")
        const elements = $scope.newElementSharing;
        $scope.newElementSharing = [];
        //hightlight
        setTimeout(() => {
            workspaceService.onChange.next({ action: "add", treeDest: "owner", elements })
            $scope.setHighlighted(elements);
        }, 300)
    }
    $scope.onSubmitSharedNewFiles = function ($event: any) {
        const founded = $scope.newElementSharing.map(e => {
            e._isShared = true;
            //restart animation
            let founded: models.Element = null;
            workspaceService.findElementInListByRefOrId($scope.openedFolder.documents, e, (f) => {
                founded = f;
            })
            return founded;
        });
        $scope.setHighlighted(founded);
        $scope.newElementSharing = [];
    }
    $scope.onCancelShareNewFilesDelete = async function () {
        template.close("lightbox")
        await workspaceService.deleteAll($scope.newElementSharing)
        $scope.newElementSharing = [];
    }
}
