import { User, ClassRoom, UserTypes } from "../model";
import { directoryService } from "../service";
import { template, idiom as lang } from "entcore";
import { EventDelegateScope } from "./events";

enum Column {
    Name,
    Birthdate,
    Login,
    Activation
}
export interface UserListDelegateScope extends EventDelegateScope {
    kinds: {
        Student: UserTypes
        Relative: UserTypes
        Teacher: UserTypes
        Personnel: UserTypes
    };
    columns: typeof Column;
    userList: { selectedTab: UserTypes, selectAll: boolean, search: string, list: User[] };
    switchAll(value?: boolean);
    usersForType(type?: UserTypes): User[]
    selectTab(kind: UserTypes);
    selectedTabCss(kind: UserTypes): string;
    isSelectedTab(kind: UserTypes): boolean;
    displayCode(user: User): string
    displayCodeCss(user: User): string
    sortAsc(column: Column, e: MouseEvent);
    sortDesc(column: Column, e: MouseEvent);
    onUserSelected()
    toggleSort(column: Column): void;
    isSortAsc(column: Column): boolean;
    isSortDesc(column: Column): boolean;
    openInfosIfNotSelected(user: User);
    //from others
    openUserInfos(user: User): void;
    exportActivationUri(): string
}
export async function UserListDelegate($scope: UserListDelegateScope) {
    // === Init template
    template.open('userList', 'admin/user-list');
    // === Init attributes
    let schoolClass: ClassRoom;
    let currentSortDir: "desc" | "asc" = "asc";
    let currentSort: Column = Column.Name;
    $scope.columns = Column;
    $scope.kinds = {
        Personnel: "Personnel",
        Relative: "Relative",
        Student: "Student",
        Teacher: "Teacher"
    };
    $scope.userList = {
        selectAll: false,
        selectedTab: "Student",
        search: '',
        list: schoolClass ? schoolClass.users : []
    };
    // === Init listeners: listen class changes
    $scope.onUserUpdate.subscribe(user => {
        schoolClass.users.filter(u => u.id == user.id).forEach(u => u.updateData(user))
    })
    $scope.onClassLoaded.subscribe((s) => {
        schoolClass = s;
        $scope.safeApply();
    })
    $scope.onClassRefreshed.subscribe((s) => {
        schoolClass = s;
        $scope.safeApply();
    })
    // === Private methods
    const sortUsers = (u1: User, u2: User) => {
        //sort by code
        if (currentSort == Column.Activation) {
            const getterName = (u: User) => {
                return u.safeDisplayName || "";
            }
            const getterCode = (u: User) => {
                //first user with reset code
                if (u.blocked) {
                    return "c";
                } else if (u.activationCode) {
                    return "b";
                    //then user blocked
                } else if (u.resetCode) {
                    return "a";
                    // then user with activation code
                } else {
                    return "d";
                }
            }
            const res = getterCode(u1).localeCompare(getterCode(u2));
            //if same => sort by name asc
            if (res == 0) {
                return getterName(u1).localeCompare(getterName(u2));
            } if (currentSortDir == "desc") {
                return -1 * res;
            } else {
                return res;
            }
        }
        // sort by other fields
        const getter = (u: User) => {
            if (!u) return "";
            switch (currentSort) {
                case Column.Name:
                    return u.safeDisplayName || "";
                case Column.Login:
                    return u.login || "";
                case Column.Birthdate:
                    return u.inverseBirthDate || "";
            }
            return "";
        }
        const value1 = getter(u1);
        const value2 = getter(u2);
        const res = value1.localeCompare(value2);
        if (currentSortDir == "desc") {
            return -1 * res;
        }
        return res;
    }
    const getSelectedUsers = function () {
        return schoolClass.users.filter(u => u.type == $scope.userList.selectedTab && u.selected);
    }
    const isTextSelected = function (): boolean {
        var text = "";
        if (typeof window.getSelection != "undefined") {
            text = window.getSelection().toString();
        }
        return text.length > 0;
    }
    // === Methods
    $scope.toggleSort = function (column) {
        if (currentSort == column) {
            currentSort = column;
            currentSortDir = currentSortDir == "asc" ? "desc" : "asc";
        } else {
            currentSort = column;
            currentSortDir = "asc";
        }
        $scope.safeApply();
    }
    $scope.isSortAsc = function (column: Column) {
        return currentSort == column && currentSortDir == "asc";
    }
    $scope.isSortDesc = function (column: Column) {
        return currentSort == column && currentSortDir == "desc";
    }
    $scope.sortAsc = function (column: Column, e) {
        e && e.stopPropagation();
        currentSort = column;
        currentSortDir = "asc";
        $scope.safeApply();
    }
    $scope.sortDesc = function (column: Column, e) {
        e && e.stopPropagation();
        currentSort = column;
        currentSortDir = "desc";
        $scope.safeApply();
    }
    $scope.usersForType = function (type = $scope.userList.selectedTab) {
        if (!schoolClass) return [];
        const filteredByType = schoolClass.users.filter(u => u.type == type);
        const filteredBySearch = directoryService.findUsers($scope.userList.search, filteredByType);
        $scope.userList.list = filteredBySearch.sort(sortUsers);
        return $scope.userList.list;
    };
    $scope.isSelectedTab = function (kind) {
        if (!kind) {
            console.warn("[Directory][UserList.isSelectedTab] kind should not be null: ", kind)
        }
        return kind == $scope.userList.selectedTab;
    }
    $scope.selectTab = function (kind) {
        $scope.switchAll(false);
        $scope.userList.search = '';
        $scope.userList.selectedTab = kind;
    }
    $scope.selectedTabCss = function (kind) {
        return $scope.isSelectedTab(kind) ? "selected" : "";
    }
    $scope.switchAll = function (value) {
        if (typeof value != "undefined") {
            $scope.userList.selectAll = value;
        }
        schoolClass.users.forEach(u => {
            if ($scope.isSelectedTab(u.type)) {
                u.selected = $scope.userList.selectAll;
            }
        });
        $scope.onSelectionChanged.next(getSelectedUsers());
    };
    $scope.onUserSelected = function () {
        $scope.onSelectionChanged.next(getSelectedUsers());
    }
    $scope.displayCode = function (user) {
        if (user.blocked) {
            return lang.translate("directory.blocked.label");
        } else if (user.activationCode) {
            return user.activationCode;
        } else if (user.resetCode) {
            return lang.translate("directory.resetted.label").replace("[[resetCode]]", user.resetCode).replace("[[resetCodeDate]]", user.resetCodeDate)
        } else {
            return lang.translate("directory.activated");
        }
    }
    $scope.displayCodeCss = function (user) {
        if (user.blocked) {
            return "blocked";
        } else if (user.activationCode) {
            return "notactivated";
        } else if (user.resetCode) {
            return "resetted"
        } else {
            return "activated";
        }
    }
    $scope.openInfosIfNotSelected = function (user) {
        if (!isTextSelected()) {
            $scope.openUserInfos(user);
        }
    }
    $scope.exportActivationUri = function () {
        if (!schoolClass || !$scope.userList.selectedTab) return "#";
        return `/directory/class/${schoolClass.id}/users?type=${$scope.userList.selectedTab.toLowerCase()}&format=csv`
    }
}