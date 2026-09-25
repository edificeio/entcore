import { model, template, FolderPickerProps, FolderPickerSourceFile, notify, idiom as lang } from "entcore";
import { models, workspaceService } from "../../services";
import { GoogleDriveDocument } from "../../directives/google-drive/models/googleDriveDocument.model";
import { googleDriveService } from "../../directives/google-drive/services/googleDrive.service";
import { SyncDocument } from "../../directives/nextcloud/models/nextcloudFolder.model";
import { nextcloudService } from "../../directives/nextcloud/services/nextcloud.service";

declare var ENABLE_GOOGLE_DRIVE: boolean;
declare var ENABLE_NEXTCLOUD: boolean;

export interface ActionCopyDelegateScope {
    copyProps: FolderPickerProps
    isMovingElementsMine(): boolean
    openCopyView()
    openMoveView()
    moveSubmit(dest: models.Element | GoogleDriveDocument | SyncDocument, elts?: models.Element[])
    copySubmit(dest: models.Element | GoogleDriveDocument | SyncDocument, elts?: models.Element[]): Promise<any>
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
    currentTree: models.ElementTree;
    trees: models.ElementTree[]
    selectedItems(): models.Element[]
    safeApply(a?)
    setHighlightTree(els: { folder: models.Node, count: number }[]);
    reloadFolderContent();
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
            // Only "Mes documents" (owner tree) is a valid copy/move destination, unlike the sidebar's full grouping.
            const ownerWrapper = {
                name: lang.translate("workspace.personal.space"),
                children: $scope.trees.filter(t => t.filter === "owner"),
                // Lets folderTree2.ts render the home icon instead of a generic folder icon.
                isPersonalSpaceWrapper: true,
            };
            return Promise.resolve([ownerWrapper] as any);
        },
        googleDriveTreeProvider: ENABLE_GOOGLE_DRIVE ? async () => {
            try {
                const rootFolder = new GoogleDriveDocument().initParent();
                // Distinct marker so only the outer node renders the Google Drive logo (both come from initParent()).
                (rootFolder as any).isGoogleDriveRootWrapper = true;
                const myDocuments = new GoogleDriveDocument().initParent();
                myDocuments.name = lang.translate("google-drive.mydrive");
                const documents = await googleDriveService.listDocument(model.me.userId);
                myDocuments.children = documents.filter(doc => doc.isFolder);
                rootFolder.children = [myDocuments];
                return [rootFolder] as any;
            } catch (e) {
                console.error("Error loading Google Drive folders", e);
                return [];
            }
        } : null,
        nextcloudTreeProvider: ENABLE_NEXTCLOUD ? async () => {
            const rootFolder = SyncDocument.createRootGroup();
            const myDocuments = new SyncDocument().initParent();
            rootFolder.children = [myDocuments];
            try {
                const documents = await nextcloudService.listDocument(model.me.userId);
                myDocuments.children = documents.filter(doc => doc.isFolder);
            } catch (e) {
                // Still show the entry (empty) rather than hiding it outright on a fetch failure.
                console.error("Error loading Nextcloud folders", e);
            }
            return [rootFolder] as any;
        } : null
    }
    const isGoogleDriveDestination = function (dest: any): dest is GoogleDriveDocument {
        return dest instanceof GoogleDriveDocument;
    }
    const isNextcloudDestination = function (dest: any): dest is SyncDocument {
        return dest instanceof SyncDocument;
    }
    // dest.path is null only for the unrenamed initParent() root node ("Mes documents" itself).
    const nextcloudDestName = function (dest: SyncDocument): string {
        return dest.path === null ? undefined : dest.name;
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
        $scope.copyProps.submit = function (dest) {
            $scope.copySubmit(dest);
        };
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
        if (isGoogleDriveDestination(dest)) {
            _moveElementsToGoogleDrive(getMovingElements(), dest);
            return;
        }
        if (isNextcloudDestination(dest)) {
            _moveElementsToNextcloud(getMovingElements(), dest);
            return;
        }
        const res = checkDest(dest, getMovingElements());
        if (res == "toshare") {
            template.open('lightbox', 'copy/move-toshare');
        } else if (res == "toown") {
            template.open('lightbox', 'copy/move-toown');
        } else if (res == "fromtoshare") {
            template.open('lightbox', 'copy/move-fromtoshare');
        } else {
            //move without feedback, but still closes the picker and refreshes the view like copySubmit's classic branch
            const toMove = getMovingElements();
            _moveElements(toMove, dest).then(() => {
                $scope.reloadFolderContent();
                closeCopyView(dest, toMove);
                $scope.safeApply()
            });
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
            // Google Drive/Nextcloud have no shared/owner distinction, so skip the toshare/toown confirmation.
            if (isGoogleDriveDestination(dest) || isNextcloudDestination(dest)) {
                return false;
            }
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
        if (isGoogleDriveDestination(dest)) {
            try {
                await googleDriveService.copyDocumentWorkspaceToCloud(
                    model.me.userId,
                    toCopy.map(elt => elt._id),
                    dest.id,
                );
                setState("finished")
                setTimeout(() => {
                    closeCopyView(null);
                    $scope.safeApply()
                }, 1000)
            } catch (e) {
                notify.error(lang.translate("google-drive.transfer.error"))
                closeCopyView(null)
            }
            return;
        }
        if (isNextcloudDestination(dest)) {
            try {
                await nextcloudService.copyDocumentWorkspaceToCloud(
                    model.me.userId,
                    toCopy.map(elt => elt._id),
                    nextcloudDestName(dest),
                );
                setState("finished")
                setTimeout(() => {
                    closeCopyView(null);
                    $scope.safeApply()
                }, 1000)
            } catch (e) {
                notify.error(lang.translate("nextcloud.transfer.error"))
                closeCopyView(null)
            }
            return;
        }
        try {
            await workspaceService.copyAll(toCopy, dest)
            $scope.reloadFolderContent();
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
            $scope.reloadFolderContent();
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
    const _moveElementsToGoogleDrive = async function (toMove: models.Element[], dest: GoogleDriveDocument) {
        setState("processing")
        $scope.isCopying = false;
        try {
            await googleDriveService.moveDocumentWorkspaceToCloud(
                model.me.userId,
                toMove.map(elt => elt._id),
                dest.id,
            );
            setState("finished")
            setTimeout(() => {
                closeCopyView(null);
                $scope.safeApply()
            }, 1000)
        } catch (e) {
            notify.error(lang.translate("google-drive.transfer.error"))
            closeCopyView(null)
        }
    }
    const _moveElementsToNextcloud = async function (toMove: models.Element[], dest: SyncDocument) {
        setState("processing")
        $scope.isCopying = false;
        try {
            await nextcloudService.moveDocumentWorkspaceToCloud(
                model.me.userId,
                toMove.map(elt => elt._id),
                nextcloudDestName(dest),
            );
            setState("finished")
            setTimeout(() => {
                closeCopyView(null);
                $scope.safeApply()
            }, 1000)
        } catch (e) {
            notify.error(lang.translate("nextcloud.transfer.error"))
            closeCopyView(null)
        }
    }
    $scope.onMoveDoMove = async function () {
        $scope.copyProps.i18.actionProcessing = "workspace.move.window.processing"
        $scope.copyProps.i18.actionFinished = "workspace.move.window.finished"
        $scope.isCopying = false;
        setState("processing")
        const toMove = [...getMovingElements()]
        try {
            // Was previously not awaited, letting closeCopyView fire while the move was still in flight.
            await _moveElements(toMove, targetFolder);
            $scope.reloadFolderContent();
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