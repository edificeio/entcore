import { template, FolderPickerProps, FolderPickerSourceFile } from "entcore";
import { models, workspaceService } from "../../services";


export interface ActionCopyDelegateScope {
    copyProps: FolderPickerProps
    openCopyView()
    openMoveView()
    //
    onMoveDoCopy()
    onMoveDoMove()
    onMoveDoCancel()
    //
    isCopyStateNormal(): boolean
    isCopyStateProcessing(): boolean
    isCopyStateFinished(): boolean
    //from others
    currentTree: models.Tree;
    trees: models.Tree[]
    selectedItems(): models.Element[]
    safeApply(a?)
    setHighlightTree(els: { folder: models.Element | models.TREE_NAME, count: number }[]);

}

export function ActionCopyDelegate($scope: ActionCopyDelegateScope) {
    let processing: "normal" | "processing" | "finished" = "normal"
    let targetFolder = null;
    const i18Copy = {
        actionFinished: "workspace.copy.window.finished",
        actionProcessing: "workspace.copy.window.processing",
        actionTitle: "workspace.copy.window.action",
        title: "workspace.copy.window.title",
        info: "workspace.copy.window.info"
    }
    const i18Move = {
        actionFinished: "workspace.move.window.finished",
        actionProcessing: "workspace.move.window.processing",
        actionTitle: "workspace.move.window.action",
        title: "workspace.move.window.title",
        info: "workspace.move.window.info"
    }
    $scope.copyProps = {
        i18: null,
        sources: [],
        onSubmitSuccess(dest, _) {
            closeCopyView(dest);
        },
        onCancel() {
            closeCopyView(null)
        },
        treeProvider() {
            return Promise.resolve($scope.trees)
        }
    }
    const checkDest = function (dest: models.Element, elts: models.Element[]) {
        const containsOwned = elts.filter(item => !item.isShared).length > 0
        const containsShared = elts.filter(item => item.isShared).length > 0
        const destIsShared = dest.isShared || (dest as models.Tree).filter == "shared";
        if (destIsShared && containsOwned) {
            return "toshare";
        } else if (!destIsShared && containsShared) {
            return "toown";
        } else {
            return "nope"
        }
    }

    $scope.openCopyView = function () {
        const cannotCopy = $scope.selectedItems().filter(f => !f.canCopy);
        if (cannotCopy.length > 0) {
            return;
        }
        //
        $scope.copyProps.i18 = i18Copy;
        $scope.copyProps.sources = $scope.selectedItems().map(s => {
            return {
                action: "copy-from-file",
                fileId: s._id
            } as FolderPickerSourceFile
        })
        $scope.copyProps.manageSubmit = null;
        $scope.copyProps.submit = null;
        template.open('lightbox', 'copy/index');
        setState("normal")
    };
    $scope.openMoveView = function () {
        const cnnotMove = $scope.selectedItems().filter(f => !f.canMove);
        if (cnnotMove.length > 0) {
            return;
        }
        //
        $scope.copyProps.i18 = i18Move;
        $scope.copyProps.sources = $scope.selectedItems().map(s => {
            return {
                action: "move-from-file",
                fileId: s._id
            } as FolderPickerSourceFile
        })
        $scope.copyProps.manageSubmit = function (dest) {
            const res = checkDest(dest, $scope.selectedItems());
            if (res == "toshare") {
                return true;
            } else if (res == "toown") {
                return true;
            } else {
                return false;
            }
        }
        $scope.copyProps.submit = function (dest) {
            targetFolder = dest;
            const res = checkDest(dest, $scope.selectedItems());
            if (res == "toshare") {
                template.open('lightbox', 'copy/move-toshare');
            } else if (res == "toown") {
                template.open('lightbox', 'copy/move-toown');
            }
        }
        template.open('lightbox', 'copy/index');
        setState("normal")
    };
    const closeCopyView = function (dest: models.Element, elts?: models.Element[]) {
        template.close("lightbox")
        $scope.copyProps.sources = [];
        //
        if (elts && checkDest(dest, elts) == "toshare") {
            $scope.setHighlightTree([{ folder: "shared", count: elts.length }])
        }
    }
    //
    $scope.onMoveDoCopy = async function () {
        $scope.copyProps.i18.actionProcessing = "workspace.copy.window.processing"
        $scope.copyProps.i18.actionFinished = "workspace.copy.window.finished"
        setState("processing")
        const toCopy = [...$scope.selectedItems()]
        await workspaceService.copyAll(toCopy, targetFolder)
        setState("finished")
        setTimeout(() => {
            closeCopyView(targetFolder, toCopy);
            $scope.safeApply()
        }, 1000)
    }
    $scope.onMoveDoMove = async function () {
        $scope.copyProps.i18.actionProcessing = "workspace.move.window.processing"
        $scope.copyProps.i18.actionFinished = "workspace.move.window.finished"
        setState("processing")
        const toMove = [...$scope.selectedItems()]
        if ($scope.currentTree.filter == "shared") {
            await workspaceService.moveAllForShared(toMove, targetFolder)
        } else {
            await workspaceService.moveAll(toMove, targetFolder)
        }
        setState("finished")
        setTimeout(() => {
            closeCopyView(targetFolder, toMove);
            $scope.safeApply()
        }, 1000)
    }
    $scope.onMoveDoCancel = function () {
        closeCopyView(null)
    }
    //
    const setState = function (state: "normal" | "processing" | "finished") {
        processing = state;
    }
    $scope.isCopyStateNormal = function () {
        return processing == "normal"
    }
    $scope.isCopyStateProcessing = function () {
        return processing == "processing"
    }
    $scope.isCopyStateFinished = function () {
        return processing == "finished"
    }

}