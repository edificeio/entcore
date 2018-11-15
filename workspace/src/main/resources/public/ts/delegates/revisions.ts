import { template, quota, $, moment, idiom as lang, notify, model } from "entcore";
import { models, workspaceService } from "../services";

export interface RevisionDelegateScope {
    //from others
    onInit(cb: () => void)
    safeApply()
    selectedDocuments(): models.Element[];
    selectedFolders(): models.Element[];
    currentTree: models.Tree
    //revisions
    targetDocument: models.Element
    order: { field: string, desc: boolean, order?: (item: models.Element) => any }
    orderByField(fieldName: string)
    createRevision(files: string[])
    openHistory(doc: models.Element);
    deleteRevision(revision: models.Revision)
    canShowRevision(): boolean
    canDeleteRevision(revision: models.Revision): boolean
    revisionInProgress: { pending?: boolean, file?: string }

}
export function RevisionDelegate($scope: RevisionDelegateScope) {
    $scope.onInit(function () {
        //INIT
        $scope.revisionInProgress = {}
        $scope.order = {
            field: 'created',
            desc: true,
            order(item) {
                if ($scope.order.field === 'created' && item.created) {
                    return moment(item.created);
                }
                if ($scope.order.field === 'name') {
                    return lang.removeAccents(item[$scope.order.field]);
                }
                if ($scope.order.field.indexOf('.') >= 0) {
                    const splitted_field = $scope.order.field.split('.')
                    let sortValue = item
                    for (let i = 0; i < splitted_field.length; i++) {
                        sortValue = typeof sortValue === 'undefined' ? undefined : sortValue[splitted_field[i]]
                    }
                    return sortValue
                } else
                    return item[$scope.order.field];
            }
        }
    });

    $scope.openHistory = async function () {
        $scope.targetDocument = $scope.selectedDocuments()[0];
        await workspaceService.syncHistory($scope.targetDocument);
        $scope.orderByField('date.$date');
        $scope.order.desc = true;
        template.open('lightbox', 'versions')
        $scope.safeApply()
    }
    $scope.createRevision = async function (newFiles) {
        if (newFiles.length < 1)
            return
        await workspaceService.createRevision(newFiles[0], $scope.targetDocument, (state, err) => {
            switch (state) {
                case "pending":
                    $scope.revisionInProgress = $scope.revisionInProgress || {}
                    $scope.revisionInProgress.pending = true
                    $scope.revisionInProgress.file = newFiles[0];
                    break;
                case "end":
                    $scope.revisionInProgress = {};
                    break;
                case "error":
                    delete $scope.revisionInProgress;
                    notify.error(err);
                    break;
            }
        })
        delete $scope.revisionInProgress;
        await quota.refresh();
        $scope.safeApply();
        template.close('lightbox');
        await workspaceService.syncHistory($scope.targetDocument);
        $scope.safeApply();

    }
    $scope.canShowRevision = function () {
        return $scope.selectedFolders().length === 0 && $scope.selectedDocuments().length === 1 && ($scope.currentTree.filter === 'shared' || $scope.currentTree.filter === 'owner');
    }
    $scope.deleteRevision = async function (revision) {
        await workspaceService.deleteRevision(revision);
        $('.tooltip').remove()
        $scope.openHistory($scope.targetDocument)
        await quota.refresh();
        $scope.safeApply();
    }
    $scope.orderByField = function (fieldName) {
        if (fieldName === $scope.order.field) {
            $scope.order.desc = !$scope.order.desc;
        }
        else {
            $scope.order.desc = false;
            $scope.order.field = fieldName;
        }
    };
    $scope.canDeleteRevision = function (revision) {
        return ($scope.targetDocument.myRights["manager"] || revision["userId"] === model.me.userId && $scope.targetDocument.myRights["contrib"]) && revision["file"] !== $scope.targetDocument.file
    }
}

