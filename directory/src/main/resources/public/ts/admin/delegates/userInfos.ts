import { EventDelegateScope } from "./events";
import { User, UserTypes, Mood, ClassRoom, School } from "../model";
import { notify } from "entcore";
import { directoryService } from "../service";

export interface UserInfosDelegateScope extends EventDelegateScope {
    openUserInfos(user: User): void;
    canSwitchToNextUser(): boolean;
    switchToNextUser(): void;
    canSwitchToPreviousUser(): boolean;
    switchToPreviousUser(): void;
    saveUserChanges();
    saveUserInfoChanges();
    saveUserBookChanges();
    onMottoChanged();
    availableMoods(): { icon: string, text: string, id: string }[];
    resetAvatar(): void;
    openLoginInput(): void;
    saveAndCloseLoginInput(): void;
    isLoginAliasWellFormatted(): boolean;
    userInfosDisplayChildren(): boolean
    userInfosDisplayRelative(): boolean
    userInfosDisplayUserbook(): boolean
    userInfoExport(user: User): void;
    userInfoExportFamily(user: User): void;
    selectedUser: User;
    mottoShouldPublish: boolean;
    showLoginInput: boolean;
    tempLoginAlias: string;
    //from others
    usersForType(type?: UserTypes): User[]
    openLightbox(path: string): void;
    userList: { selectedTab: UserTypes, selectAll: boolean, search: string, list: User[] };
}

export async function UserInfosDelegate($scope: UserInfosDelegateScope) {
    //init
    $scope.mottoShouldPublish = false;
    $scope.showLoginInput = false;
    // === Private attributs
    let _classroom: ClassRoom;
    let _schools: School[] = [];
    let list = [];
    let index = -1;
    //=== Listeners
    $scope.onClassLoaded.subscribe((s) => {
        _classroom = s;
    })
    $scope.onSchoolLoaded.subscribe(loaded => {
        _schools = loaded;
    });
    //=== Private methods
    const getSchool = function () {
        const school = _classroom && _schools.find(sc => !!sc.classrooms.find(clazz => clazz.id == _classroom.id));
        if (!school) {
            console.warn("[Directory][UserInfos.getSchool] school should not be undefined", _classroom, school)
        }
        return school;
    }
    const setSelectedUser = async (user: User) => {
        $scope.mottoShouldPublish = false;
        $scope.showLoginInput = false;
        await user.open({ withChildren: true });
        $scope.selectedUser = user;
        $scope.selectedUser.picture = user.picture || user.avatarUri;
        $scope.safeApply();
    }
    const selectFirstUser = async (user: User) => {
        await setSelectedUser(user);
        list = $scope.usersForType();
        index = list.indexOf($scope.selectedUser);
    }
    const selectNext = async () => {
        if (!$scope.canSwitchToNextUser()) return;
        const next = list[index + 1];
        index = index + 1;
        await setSelectedUser(next);
        return next;
    }
    const selectPrevious = async () => {
        if (!$scope.canSwitchToPreviousUser()) return;
        const prev = list[index - 1];
        index = index - 1;
        await setSelectedUser(prev);
        return prev;
    }
    // === Public methods
    $scope.openUserInfos = async function (user: User) {
        try {
            await selectFirstUser(user);
            $scope.showLoginInput = false;
            $scope.openLightbox('admin/user-infos');
            $scope.safeApply();
        } catch (e) {
            notify.error('unexpected.error');
        }
    }

    $scope.onMottoChanged = function () {
        $scope.mottoShouldPublish = true;
    }
    //
    $scope.canSwitchToNextUser = function () {
        return index < (list.length - 1);
    }
    $scope.switchToNextUser = async function () {
        selectNext();
    }
    $scope.canSwitchToPreviousUser = function () {
        return index > 0;
    }
    $scope.switchToPreviousUser = async function () {
        selectPrevious();
    }
    $scope.saveUserChanges = async function () {
        await directoryService.updateUser($scope.selectedUser, { withUserBook: true, withInfos: true });
        $scope.onUserUpdate.next($scope.selectedUser);
    }
    $scope.saveUserBookChanges = async function () {
        $scope.mottoShouldPublish = false;
        await directoryService.updateUser($scope.selectedUser, { withUserBook: true, withInfos: false });
        $scope.onUserUpdate.next($scope.selectedUser);
    }
    $scope.saveUserInfoChanges = function () {
        directoryService.updateUser($scope.selectedUser, { withUserBook: false, withInfos: true });
    }
    const moods = Mood.availableMoods();
    $scope.availableMoods = function () {
        return moods;
    }
    $scope.resetAvatar = function () {
        $scope.selectedUser.picture = '';
        $scope.saveUserBookChanges();
    };
    $scope.openLoginInput = function () {
        $scope.selectedUser.tempLoginAlias = $scope.selectedUser.login;
        $scope.showLoginInput = true;
    }
    $scope.userInfoExport = function (user) {
        directoryService.generateReport({
            ids: [user.id],
            structureId: getSchool().id,
            type: "pdf"
        })
    }
    $scope.userInfoExportFamily = function (user) {
        const userIds = $scope.selectedUser.relatives.map(u => u.id);
        directoryService.generateReport({
            ids: [...userIds, user.id],
            structureId: getSchool().id,
            type: "simplePdf"
        })
    }
    let savingLogin = false;
    $scope.saveAndCloseLoginInput = async function () {
        if (savingLogin) return;
        try {
            savingLogin = true;
            if ($scope.selectedUser.login !== $scope.selectedUser.tempLoginAlias) {
                await directoryService.updateUserLoginAlias($scope.selectedUser);
                $scope.showLoginInput = false;
                if ($scope.selectedUser.tempLoginAlias.length === 0) {
                    $scope.selectedUser.login = $scope.selectedUser.originalLogin;
                } else {
                    $scope.selectedUser.login = $scope.selectedUser.tempLoginAlias;
                }
                $scope.safeApply();
            }
        } catch (e) {
            notify.error('directory.notify.loginUpdate.error.alreadyExists');
        } finally {
            savingLogin = false;
        }
    }
    $scope.isLoginAliasWellFormatted = function () {
        return (/^[a-z\d\.-]*$/).test($scope.selectedUser.tempLoginAlias);
    }
    $scope.userInfosDisplayChildren = function () {
        return $scope.selectedUser && $scope.selectedUser.type == "Relative";
    }
    $scope.userInfosDisplayRelative = function () {
        return $scope.selectedUser && $scope.selectedUser.type == "Student";
    }
    $scope.userInfosDisplayUserbook = function () {
        return $scope.selectedUser && !$scope.selectedUser.activationCode;
    }
}
