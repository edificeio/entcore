import { angular, Me, notify } from "entcore";
import { Observable, Subject } from "rxjs";
import { ClassRoom, Mood, School, User, UserTypes } from "../model";
import { directoryService } from "../service";
import { EventDelegateScope, TRACK } from "./events";

interface DropdownOption {
    innerObject: User;
    type?: string
    structureName?: string
    profile: UserTypes
    toString(): string
}

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
    openDisplayNameInput(): void;
    saveAndCloseDisplayNameInput(): void;
    isDisplayNameWellFormatted(): boolean;
    openEmailInput(): void;
    saveAndCloseEmailInput(): void;
    isEmailWellFormatted(): boolean;
    openPhoneInput(): void;
    saveAndClosePhoneInput(): void;
    isPhoneWellFormatted(): boolean;
    openMobileInput(): void;
    saveAndCloseMobileInput(): void;
    isMobileWellFormatted(): boolean;
    userInfosDisplayChildren(): boolean
    userInfosDisplayRelative(): boolean
    userInfosDisplayUserbook(): boolean
    userInfoExport(user: User): void;
    userInfoExportFamily(user: User): void;
    onUserInfosSearchChange(): void;
    onUserInfosSearchClean(): void;
    onUserInfosSearchSelect(): void;
    onUserInfosRemoveChild(user: User): void; 
    userInfos: {
        searching: boolean;
        linking: boolean,
        search: string,
        results: DropdownOption[],
        select: DropdownOption,
        linked: User[]
    };
    selectedUser: User;
    mottoShouldPublish: boolean;
    showLoginInput: boolean;
    temp: {
        displayName?: string;
        email?: string;
        homePhone?: string;
        mobile?: string;
        // International phone number
        intlFormatNumber?: () => string
    };
    showDisplayNameInput: boolean;
    showEmailInput: boolean;
    showPhoneInput: boolean;
    showMobileInput: boolean;
    isForbidden(): boolean;
    //from others
    usersForType(type?: UserTypes): User[]
    openLightbox(path: string): void;
    userList: { selectedTab: UserTypes, selectAll: boolean, search: string, list: User[] };
    selectedSchoolId(classroom: ClassRoom): string;
    selectedClass: ClassRoom;
}

export async function UserInfosDelegate($scope: UserInfosDelegateScope) {
    //init
    $scope.mottoShouldPublish = false;
    $scope.showLoginInput = false;
    $scope.showEmailInput = false;
    $scope.temp = {};
    $scope.userInfos = {
        linking: false,
        searching: false,
        search: '',
        select: null,
        results: [],
        linked: []
    }

    // === Private attributs
    const onSearchChange = new Subject<string>();
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
    (onSearchChange as Observable<string>).distinctUntilChanged().debounceTime(450).subscribe(value => {
        search(value);
    });
    //=== Private methods
    const getSchool = function () {
        const school = _classroom && _schools.find(sc => !!sc.classrooms.find(clazz => clazz.id == _classroom.id));
        if (!school) {
            console.warn("[Directory][UserInfos.getSchool] school should not be undefined", _classroom, school)
        }
        return school;
    }
    /**
    * Reset all modifications and flags that were started by the users.
    *
    **/
    const resetModifications = () => {
        $scope.temp.displayName = "";
        $scope.showDisplayNameInput = false;
        $scope.selectedUser.tempLoginAlias = "";
        $scope.showLoginInput = false;
        $scope.temp.email = "";
        $scope.showEmailInput = false;
        $scope.temp.homePhone = "";
        $scope.showPhoneInput = false;
        $scope.temp.mobile = "";
        $scope.showMobileInput = false;
        cleanSearch();
    }
    const setSelectedUser = async (user: User) => {
        $scope.mottoShouldPublish = false;
        await user.open({ withChildren: true });
        $scope.selectedUser = user;
        $scope.selectedUser.picture = user.picture || user.avatarUri;
        resetModifications();
        $scope.userInfos.linked = [];
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
            $scope.showEmailInput = false;
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
        });
        // #47174, Track this event
        $scope.tracker.trackEvent( TRACK.event, TRACK.PUBLIPOSTAGE.action, TRACK.name(TRACK.PUBLIPOSTAGE.DETAILED_PDF_ONE, user.type), 1 );
    }
    $scope.userInfoExportFamily = function (user) {
        const userIds = $scope.selectedUser.relatives.map(u => u.id);
        directoryService.generateReport({
            ids: [...userIds, user.id],
            structureId: getSchool().id,
            type: "simplePdf"
        });
    }
    let savingLogin = false;
    $scope.saveAndCloseLoginInput = async function () {
        if (savingLogin) return;
        try {
            savingLogin = true;
            if ($scope.selectedUser.login !== $scope.selectedUser.tempLoginAlias) {
                // #47174, Track this event
                $scope.tracker.trackEvent( TRACK.event, TRACK.AUTH_MODIFICATION.action, TRACK.name(TRACK.AUTH_MODIFICATION.ID_USER, $scope.selectedUser.type), 1 );
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
    $scope.openDisplayNameInput = function () {
        $scope.temp.displayName = $scope.selectedUser.displayName;
        $scope.showDisplayNameInput = true;
    }
    let savingDisplayName = false;
    $scope.saveAndCloseDisplayNameInput = async function () {
        if (savingDisplayName) return;
        try {
            savingDisplayName = true;
            if ($scope.selectedUser.displayName !== $scope.temp.displayName && $scope.isDisplayNameWellFormatted()) {
                $scope.selectedUser.displayName = $scope.temp.displayName;
                await directoryService.updateUserDisplayName($scope.selectedUser);
                $scope.showDisplayNameInput = false;
                $scope.safeApply();
            }
        } catch (e) {
            notify.error('directory.form.displayName');
        } finally {
            savingDisplayName = false;
        }
    }
    $scope.isDisplayNameWellFormatted = function () {
        return angular.element("input[type=\"text\"][name=\"tempDisplayName\"]").hasClass('ng-valid');
    }
    $scope.openEmailInput = function () {
        // An ADML changing his own email address must be redirected to "my account"
        if( Me.session.functions.ADMIN_LOCAL && $scope.selectedUser.id == Me.session.userId ) {
            window.location.href = "/userbook/mon-compte#/edit-me";
            return;
        }
        $scope.temp.email = $scope.selectedUser.email;
        $scope.showEmailInput = true;
    }
    let savingEmail = false;
    $scope.saveAndCloseEmailInput = async function () {
        if (savingEmail) return;
        try {
            savingEmail = true;
            if ($scope.selectedUser.email !== $scope.temp.email && $scope.isEmailWellFormatted()) {
                $scope.selectedUser.email = $scope.temp.email;
                await directoryService.updateUserEmail($scope.selectedUser);
                $scope.showEmailInput = false;
                $scope.safeApply();
            }
        } catch (e) {
            notify.error('directory.form.email');
        } finally {
            savingEmail = false;
        }
    }
    $scope.isEmailWellFormatted = function () {
        return angular.element("input[type=\"email\"][name=\"tempEmail\"]").hasClass('ng-valid');
    }
    $scope.openPhoneInput = function () {
        $scope.temp.homePhone = $scope.selectedUser.homePhone;
        $scope.showPhoneInput = true;
    }
    let savingPhone = false;
    $scope.saveAndClosePhoneInput = async function () {
        if (savingPhone) return;
        try {
            savingPhone = true;
            if ($scope.selectedUser.homePhone !== $scope.temp.homePhone && $scope.isPhoneWellFormatted()) {
                $scope.selectedUser.homePhone = $scope.temp.homePhone;
                await directoryService.updateUserHomePhone($scope.selectedUser);
                $scope.showPhoneInput = false;
                $scope.safeApply();
            }
        } catch (e) {
            notify.error('directory.form.phone');
        } finally {
            savingPhone = false;
        }
    }
    $scope.isPhoneWellFormatted = function () {
        return angular.element("input[type=\"tel\"][name=\"tempPhone\"]").hasClass('ng-valid');
    }
    $scope.openMobileInput = function () {
         // An ADML changing his own phone numer must be redirected to "my account"
         if( Me.session.functions.ADMIN_LOCAL && $scope.selectedUser.id == Me.session.userId ) {
            window.location.href = "/userbook/mon-compte#/edit-me";
            return;
        }
        $scope.temp.mobile = $scope.selectedUser.mobile;
        $scope.showMobileInput = true;
    }
    let savingMobile = false;
    $scope.saveAndCloseMobileInput = async function () {
        if (savingMobile) return;
        try {
            savingMobile = true;
            if ($scope.temp.intlFormatNumber) {
                $scope.temp.mobile = $scope.temp.intlFormatNumber();
            }
            if ($scope.selectedUser.mobile !== $scope.temp.mobile && $scope.isMobileWellFormatted()) {
                $scope.selectedUser.mobile = $scope.temp.mobile;
                await directoryService.updateUserMobile($scope.selectedUser);
                $scope.showMobileInput = false;
                $scope.safeApply();
            }
        } catch (e) {
            notify.error('directory.form.phone');
        } finally {
            savingMobile = false;
        }
    }
    $scope.isMobileWellFormatted = function () {
        return angular.element("input[type=\"tel\"][name=\"mobile\"]").hasClass('ng-valid');
    }
    $scope.isForbidden = function() {
        if( !Me.session.functions.SUPER_ADMIN 
            && $scope.selectedUser && $scope.selectedUser.lockedEmail
            && $scope.selectedUser.id != Me.session.userId ) {
            return true;
        }
        return false;
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

    const cleanSearch = () => {
        $scope.userInfos.search = "";
        $scope.userInfos.results = [];
        $scope.userInfos.select = null;
        $scope.userInfos.searching = false;
    }
    const search = async (query: string) => {
        try {
            if (query && query.length >= 1) {
                $scope.userInfos.results = [];
                $scope.userInfos.searching = true;
                const founded = await directoryService.searchInDirectory(query, {
                    structures: [$scope.selectedSchoolId($scope.selectedClass)],
                    profiles: ["Student"]
                });
                $scope.userInfos.results = founded
                .filter((f:User) => ($scope.selectedUser.relativeList.findIndex(relative=>relative.relatedId === f.id) === -1 && !$scope.userInfos.linked.map(u=>u.id).includes(f.id) ))
                .map(f => ({
                    innerObject: f as User,
                    profile: "Student" as UserTypes,
                    toString: () => (f as User).displayName
                }));
                $scope.safeApply();
            }
        } finally {
            $scope.userInfos.searching = false;
        }
    }
    $scope.onUserInfosSearchChange = function () {
        onSearchChange.next($scope.userInfos.search);
    }
    $scope.onUserInfosSearchClean = function () {
        cleanSearch();
    }
    $scope.onUserInfosSearchSelect = async function () {
        if($scope.userInfos.linking) return;
        $scope.userInfos.linking = true;
        try {
            if ($scope.userInfos.select && $scope.userInfos.select.innerObject) {
                const student = await directoryService.linkStudentToRelative($scope.selectedUser, $scope.userInfos.select.innerObject);
                $scope.userInfos.linked.push(student);
                $scope.userInfos.results =$scope.userInfos.results.filter(u=> u.innerObject.id !== student.id)
                $scope.safeApply();
            }
        }
        finally {
            $scope.userInfos.linking = false;
        }
    }
    $scope.onUserInfosRemoveChild = async function (user) {
        if($scope.userInfos.linking) return;
        $scope.userInfos.linking = true;
        try {
            await directoryService.unlinkStudentToRelative($scope.selectedUser, user);
            $scope.userInfos.linked = $scope.userInfos.linked.filter(u=>u.id!==user.id);
            $scope.safeApply();
        }
        finally {
            $scope.userInfos.linking = false;
        }
    }
}
