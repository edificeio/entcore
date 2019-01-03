import { model, template, FolderPickerProps, FolderPickerSourceFile, notify } from "entcore";
import { models, workspaceService } from "../../services";


export interface ActionCopyDelegateScope {
    copyProps: FolderPickerProps
    isMovingElementsMine(): boolean
    openCopyView()
    openMoveView()
    moveSubmit(dest: models.Element, elts?: models.Element[])
    copySubmit(dest: models.Element, elts?: models.Element[]): Promise<any>
    //
    onMoveDoCopy()
    onMoveDoMove()
    onMoveDoCancel()
    //
    isCopyStateNormal(): boolean
    isCopyStateProcessing(): boolean
    isCopyStateFinished(): boolean
    isCopying: boolean
    //from others
    currentTree: models.Tree;
    trees: models.Tree[]
    selectedItems(): models.Element[]
    safeApply(a?)
    setHighlightTree(els: { folder: models.Node, count: number }[]);

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
        onSubmitSuccess(dest, count) {
            closeCopyView(dest, null, count);
        },
        onCancel() {
            closeCopyView(null)
        },
        onError(e) {
            closeCopyView(null);
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
        } else if (destIsShared && containsShared) {
            return "fromtoshare";
        } else {
            return "nope"
        }
    }
    let movingItems: models.Element[] = null;
    const getMovingElements = function () {
        if (movingItems == null) {
            return $scope.selectedItems();
        }
        return movingItems;
    }
    $scope.openCopyView = function () {
        movingItems = null;//get moving elements from selection
        const cannotCopy = getMovingElements().filter(f => !f.canCopy);
        if (cannotCopy.length > 0) {
            return;
        }
        //
        $scope.copyProps.i18 = i18Copy;
        $scope.copyProps.sources = getMovingElements().map(s => {
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
    $scope.moveSubmit = function (dest, elements = null) {
        if (elements) {
            //not passing through move view
            movingItems = elements;
            $scope.copyProps.i18 = i18Move;
            setState("normal")
        }
        targetFolder = dest;
        const res = checkDest(dest, getMovingElements());
        if (res == "toshare") {
            template.open('lightbox', 'copy/move-toshare');
        } else if (res == "toown") {
            template.open('lightbox', 'copy/move-toown');
        } else if (res == "fromtoshare") {
            template.open('lightbox', 'copy/move-fromtoshare');
        } else {
            //move without feedback
            _moveElements(getMovingElements(), dest);
        }
    }

    $scope.openMoveView = function () {
        movingItems = null;//get moving elements from selection
        const cnnotMove = getMovingElements().filter(f => !f.canMove);
        if (cnnotMove.length > 0) {
            return;
        }
        //
        $scope.copyProps.i18 = i18Move;
        $scope.copyProps.sources = getMovingElements().map(s => {
            return {
                action: "move-from-file",
                fileId: s._id
            } as FolderPickerSourceFile
        })
        $scope.copyProps.manageSubmit = function (dest) {
            const res = checkDest(dest, getMovingElements());
            if (res == "toshare") {
                return true;
            } else if (res == "toown") {
                return true;
            } else if (res == "fromtoshare") {
                return true;
            } else {
                return false;
            }
        }
        $scope.copyProps.submit = function (dest) {
            $scope.moveSubmit(dest)
        }
        template.open('lightbox', 'copy/index');
        setState("normal")
    };
    const closeCopyView = function (dest: models.Element, elts?: models.Element[], count?: number) {
        template.close("lightbox")
        $scope.copyProps.sources = [];
        //
        if (elts || count) {
            $scope.setHighlightTree([{ folder: dest, count: elts ? elts.length : count }])
        }
    }
    //
    $scope.copySubmit = async function (dest, elements = null) {
        if (elements) {
            //not passing through copy view
            movingItems = elements;
            $scope.copyProps.i18 = i18Copy;
            template.open('lightbox', 'copy/copy-spinner');
            setState("processing")
            $scope.safeApply()
        }
        $scope.isCopying = true;
        setState("processing")
        const toCopy = [...getMovingElements()]
        try {
            await workspaceService.copyAll(toCopy, dest)
            setState("finished")
            setTimeout(() => {
                closeCopyView(dest, toCopy);
                $scope.safeApply()
            }, 1000)
        } catch (e) {
            closeCopyView(null)
        }
    }
    $scope.onMoveDoCopy = async function () {
        $scope.copyProps.i18.actionProcessing = "workspace.copy.window.processing"
        $scope.copyProps.i18.actionFinished = "workspace.copy.window.finished"
        $scope.isCopying = true;
        setState("processing")
        const toCopy = [...getMovingElements()]
        try {
            await workspaceService.copyAll(toCopy, targetFolder)
            setState("finished")
            setTimeout(() => {
                closeCopyView(targetFolder, toCopy);
                $scope.safeApply()
            }, 1000)
        } catch (e) {
            closeCopyView(null)
        }
    }
    const _moveElements = async function (toMove: models.Element[], targetFolder: models.Element) {
        if ($scope.currentTree.filter == "shared") {
            const direction = checkDest(targetFolder, toMove);
            switch (direction) {
                case "toown":
                    await workspaceService.moveAllForShared(toMove, targetFolder);
                    break;
                default:
                    await workspaceService.moveAll(toMove, targetFolder)
                    break;
            }
        } else {
            await workspaceService.moveAll(toMove, targetFolder)
        }
    }
    $scope.onMoveDoMove = async function () {
        $scope.copyProps.i18.actionProcessing = "workspace.move.window.processing"
        $scope.copyProps.i18.actionFinished = "workspace.move.window.finished"
        $scope.isCopying = false;
        setState("processing")
        const toMove = [...getMovingElements()]
        try {
            _moveElements(toMove, targetFolder);
            setState("finished")
            setTimeout(() => {
                closeCopyView(targetFolder, toMove);
                $scope.safeApply()
            }, 1000)
        } catch (e) {
            closeCopyView(null)
        }
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
    $scope.isCopying = false;
    $scope.isMovingElementsMine = function () {
        return getMovingElements().filter(m => {
            const userId: any = m.owner.userId || m.owner
            return userId != model.me.userId;
        }).length == 0;
    }

}