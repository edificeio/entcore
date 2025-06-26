import { User, ClassRoom, UserTypes } from "../model";
import { EventDelegateScope, TRACK } from "./events";
import { template, notify, idiom } from "entcore";
import { directoryService } from '../service';

export interface ActionsDelegateScope extends EventDelegateScope {
    hasSelectedUsers(): boolean;
    selectedUsersAreActivated(): boolean;
    selectedUsersAreNotActivated(): boolean;
    selectedUsersAreBlocked(): boolean;
    selectedUsersAreNotBlocked(): boolean;
    selectedUsersAreFederated(): boolean;
    confirmRemove();
    canRemoveSelection(): boolean
    blockUsers(): void;
    unblockUsers(): void;
    unlinkSelectedUsers(trackEvent?:boolean): void;
    unlinkTextAction(): string;
    deleteTextAction(): string;
    generateTemporaryPasswords(): void;
    resetPasswordUsers: User[];
    // from others
    userList: { selectedTab: UserTypes, selectAll: boolean, search: string, list: User[] };
    selectedClass: ClassRoom;
    reloadClassroom(classroom: ClassRoom): void;
    openLightbox(name: string): void
    closeLightbox(): void;
    printResetPasswordUsers(): void;
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
    $scope.selectedUsersAreActivated = function () {
        return selection.findIndex((u) => !!u.activationCode) == -1;
    }
    $scope.selectedUsersAreNotActivated = function () {
        return selection.findIndex((u) => !u.activationCode) == -1;
    }
    $scope.selectedUsersAreNotBlocked = function () {
        return selection.findIndex((u) => u.blocked) == -1;
    }
    $scope.selectedUsersAreBlocked = function () {
        return selection.findIndex((u) => !u.blocked) == -1;
    }
    $scope.selectedUsersAreFederated = function () {
        return selection.findIndex((u) => !(u.federatedIDP && u.federated === true)) === -1;
    }
    $scope.canRemoveSelection = function () {
        return selection.filter((user) => {
            return user.source != 'MANUAL' && user.source != 'CLASS_PARAM' && user.source != 'BE1D' && user.source != 'CSV'
        }).length == 0;
    }
    $scope.confirmRemove = function () {
        // #47174, Track this event
        $scope.tracker.trackEvent(TRACK.event, TRACK.USER_BLOCK.action, TRACK.name(TRACK.USER_BLOCK.SUPPRESS_SUPPRESS, $scope.userList.selectedTab), selection.length );
        directoryService.removeUsers(selection).then(() => {
            $scope.reloadClassroom($scope.selectedClass);
            // selection is emptied as all selected users were removed
            selection.splice(0, selection.length);
            notify.info('classAdmin.delete.success');
        }).catch(() => notify.error('classAdmin.delete.error'));
        template.close('lightbox');
    }
    $scope.blockUsers = function () {
        // #47174, Track this event
        $scope.tracker.trackEvent(TRACK.event, TRACK.USER_BLOCK.action, TRACK.name(TRACK.USER_BLOCK.BLOCK) );
        directoryService.blockUsers(true, selection).then(() => {
            $scope.reloadClassroom($scope.selectedClass);
            selection.splice(0, selection.length);
            notify.info('classAdmin.block.success');
        }).catch(() => notify.error('classAdmin.block.error'));
        template.close('lightbox');
    }
    $scope.unblockUsers = function () {
        // #47174, Track this event (addendum 10-2021)
        $scope.tracker.trackEvent(TRACK.event, TRACK.USER_BLOCK.action, TRACK.name(TRACK.USER_BLOCK.UNBLOCK) );
        directoryService.blockUsers(false, selection).then(() => {
            $scope.reloadClassroom($scope.selectedClass);
            selection.splice(0, selection.length);
            notify.info('classAdmin.unblock.success');
        }).catch(() => notify.error('classAdmin.unblock.error'));
        template.close('lightbox');
    }
    $scope.deleteTextAction = () => {
        const text = idiom.translate("classAdmin.delete.text") as string;
        return text.replace("[[selectionCount]]", selection.length + "").replace("[[currentClass]]", $scope.selectedClass.name);
    }
    $scope.unlinkTextAction = () => {
        const text = idiom.translate("classAdmin.unlink.text") as string;
        return text.replace("[[selectionCount]]", selection.length + "").replace("[[currentClass]]", $scope.selectedClass.name);
    }
    $scope.unlinkSelectedUsers = async (trackEvent?:boolean) => {
        try {
            // #47174, Track this event
            trackEvent && $scope.tracker.trackEvent(
                TRACK.event, 
                TRACK.USER_BLOCK.action, 
                TRACK.name(TRACK.USER_BLOCK.SUPPRESS_REMOVE_CLASS, $scope.userList.selectedTab),
                selection.length
            );
            const classid = $scope.selectedClass.id;
            const promise = directoryService.unlinkUsersFromClass(selection, classid, { withRelative: true });
            template.close('lightbox');
            await promise;
            notify.info('classAdmin.unlink.success');
            $scope.queryClassRefresh.next($scope.selectedClass);
        } catch (e) {
            notify.info('classAdmin.unlink.error');
        } finally {
        }
    }
    $scope.generateTemporaryPasswords = async function() {
		let resetUsers = selection.filter((u) => !u.activationCode);
		if (resetUsers.length != 0) {
            // #47174, Track this event
            $scope.tracker.trackEvent( TRACK.event, TRACK.AUTH_MODIFICATION.action, TRACK.name(TRACK.AUTH_MODIFICATION.CODE) );
			let resetCodes = (await directoryService.generateResetCodes(resetUsers)).data;
            resetUsers.forEach(u => {
                u.resetCode = resetCodes[u.id].code;
                u.resetCodeDate = resetCodes[u.id].date;
            });
        }
        $scope.resetPasswordUsers = selection;
		$scope.openLightbox("resetpassword");
	}
}