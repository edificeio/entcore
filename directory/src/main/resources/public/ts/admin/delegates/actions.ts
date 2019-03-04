import { User, ClassRoom } from "../model";
import { EventDelegateScope } from "./events";
import { template, notify } from "entcore";
import { directoryService } from '../service';

export interface ActionsDelegateScope extends EventDelegateScope {
    hasSelectedUsers(): boolean;
    selectedUsersAreNotActivated(): boolean;
    selectedUsersAreBlocked(): boolean;
    selectedUsersAreNotBlocked(): boolean;
    confirmRemove();
    canRemoveSelection(): boolean
    blockUsers(): void;
    unblockUsers(): void;
    // from others
    selectedClass: ClassRoom;
    reloadClassroom(classroom: ClassRoom): void;
}
export function ActionsDelegate($scope: ActionsDelegateScope) {
    // === Init template
    template.open('toaster', 'admin/toaster');
    // === Private attributes
    let selection: User[] = [];
    // === Init listener: listen selection changes
    $scope.onSelectionChanged.subscribe(s => {
        selection = s;
    })
    // === Methods
    $scope.hasSelectedUsers = function () {
        return selection.length > 0;
    }
    $scope.selectedUsersAreNotActivated = function () {
        return selection.findIndex((u) => !!u.activationCode) == -1;
    }
    $scope.selectedUsersAreNotBlocked = function () {
        return selection.findIndex((u) => u.blocked) == -1;
    }
    $scope.selectedUsersAreBlocked = function () {
        return selection.findIndex((u) => !u.blocked) == -1;
    }
    $scope.canRemoveSelection = function () {
        return selection.filter((user) => {
            return user.source != 'MANUAL' && user.source != 'CLASS_PARAM' && user.source != 'BE1D' && user.source != 'CSV'
        }).length == 0;
    }
    $scope.confirmRemove = function () {
        directoryService.removeUsers(selection).then(() => {
            $scope.reloadClassroom($scope.selectedClass);
            // selection is emptied as all selected users were removed
            selection.splice(0,selection.length);
            notify.info('classAdmin.delete.success');
        }).catch(() => notify.error('classAdmin.delete.error'));
        template.close('lightbox');
    }
    $scope.blockUsers = function () {
        directoryService.blockUsers(true,selection).then(() => {
            $scope.reloadClassroom($scope.selectedClass);
            selection.splice(0,selection.length);
            notify.info('classAdmin.block.success');
        }).catch(() => notify.error('classAdmin.block.error'));
        template.close('lightbox');
    }
    $scope.unblockUsers = function () {
        directoryService.blockUsers(false,selection).then(() => {
            $scope.reloadClassroom($scope.selectedClass);
            selection.splice(0,selection.length);
            notify.info('classAdmin.unblock.success');
        }).catch(() => notify.error('classAdmin.unblock.error'));
        template.close('lightbox');
    }
}