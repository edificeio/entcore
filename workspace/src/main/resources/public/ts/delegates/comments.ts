import { template, $, model } from "entcore";
import { models, workspaceService } from "../services";


export interface CommentDelegateScope {
    //from others
    currentTree: models.Tree;
    selectedItems(): models.Element[]
    onInit(cab: () => void);
    safeApply()
    //

    //comments
    targetElement: models.Element
    //comment
    folderComment: models.Element
    documentComment: models.Element
    canShowBadgeComments(current: models.Element): boolean
    canShowCommentLightbox(current: models.Element): boolean
    canDeleteComment(current: models.Element, comment: models.Comment): boolean
    sendComment()
    openCommentView()
    removeComment(item: models.Element, comment: models.Comment)
    toggleComments()
    isCommentVisible(): boolean;
    canComment(): boolean;
    canShowComments(): boolean;
    commentCount(): number;
    showComments(el: models.Element, event?: any)
    showFolderComments(el: models.Element, event?: any)
}
export function CommentDelegate($scope: CommentDelegateScope) {
    $scope.onInit(function () {
        //INIT
        $scope.targetElement = models.emptyDoc();
    });
    $scope.canShowBadgeComments = function (current: models.Element) {
        return current.comments.length > 0 && $scope.currentTree.name !== 'trash'
    }
    $scope.canShowCommentLightbox = function (current: models.Element) {
        return current.showComments && $scope.selectedItems()[0] === current;
    }
    $scope.canDeleteComment = function (current: models.Element, comment: models.Comment) {
        return comment.id && ((comment.author === model.me.userId && current.myRights["comment"]) || current.myRights["manager"])
    }
    $scope.toggleComments = function () {
        if ($scope.selectedItems().length > 0) {
            let document = $scope.selectedItems()[0];
            document.showComments = !document.showComments;
        }
    };
    $scope.isCommentVisible = function () {
        let document: models.Element = null;
        if ($scope.selectedItems().length > 0) {
            document = $scope.selectedItems()[0];
        }
        return document && document.showComments;
    };

    $scope.showComments = $scope.showFolderComments = function (document, $event) {
        if ($event) {
            $event.preventDefault();
        }
        $scope.targetElement = document;
        $scope.selectedItems().forEach(folder => {
            folder.selected = false;
            folder.showComments = false;
        });

        document.selected = true;
        document.showComments = true;
    }


    $scope.removeComment = async function (item, comment) {
        await workspaceService.removeComment(item, comment);
        item.comments.splice(item.comments.indexOf(comment), 1)
        $scope.safeApply()
    }

    $scope.sendComment = async function () {
        template.close('lightbox');
        const comment = await workspaceService.sendComment($scope.targetElement);
        $scope.targetElement.comments = $scope.targetElement.comments || [];
        $scope.targetElement.comments.push(comment);
        $scope.documentComment = $scope.targetElement;
        $scope.targetElement.comment = "";
        $scope.safeApply();
    };

    $scope.openCommentView = function () {
        let document = $scope.selectedItems()[0];
        if (document) {
            $scope.targetElement = document;
            template.open('lightbox', 'comment');
        }
    };

    $scope.canShowComments = function () {
        return $scope.selectedItems().length === 1 && $scope.selectedItems()[0].comments.length > 0 && $scope.currentTree.filter !== 'trash';
    }

    $scope.canComment = function () {
        return $scope.selectedItems().length === 1 && ($scope.currentTree.filter === 'shared' || $scope.currentTree.filter === 'owner');
    }

    $scope.commentCount = function () {
        let doc = $scope.selectedItems()[0]
        return doc && doc.comments ? doc.comments.length : 0;
    }
}

