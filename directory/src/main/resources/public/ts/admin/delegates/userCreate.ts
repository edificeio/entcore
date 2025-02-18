import { idiom as lang, notify } from "entcore";
import { Observable, Subject } from "rxjs";
import { ClassRoom, User, UserTypes } from "../model";
import { directoryService } from "../service";
import { EventDelegateScope, TRACK } from "./events";
import moment = require("moment");

export enum UserCreateField {
    Name,
    FName,
    Birthdate,
    Mail,
    Type
}

interface DropdownOption {
    innerObject: User;
    type?: string
    structureName?: string
    profile: UserTypes
    toString(): string
}

export interface UserCreateDelegateScope extends EventDelegateScope {
    fields: typeof UserCreateField;
    userCreate: {
        addAfterSubmit: boolean
        searching: boolean
        duplicates: User[]
        userToAttach: User
        form: User,
        search: string,
        results: DropdownOption[],
        select: DropdownOption,
        minDate: Date,
        maxDate: Date,
        submitting: boolean,
        checkRelations: boolean
    };
    classnameForDuplicateUser(user: User): string;
    onUserCreateBlur(field: UserCreateField): void;
    onUserCreateSearchClean(): void
    onUserCreateRemoveChild(user: User);
    onUserCreateSearchSelect(): void;
    onUserCreateSearchChange();
    canUserCreateSubmit(form): boolean;
    isUserCreateSearchVisible(): boolean;
    goToImportUser(): void;
    goToCreateUserForm(clean?:boolean): void;
    goToDuplicateUserForm(): void;
    goToWarnNoRelatives(): void;
    isUserAttachSelected(user: User): boolean;
    isUserCreateRequired(field: UserCreateField): boolean;
    onUserCreateChange(field: UserCreateField): void;
    submitUserCreateThenAdd(): void;
    submitUserCreateThenQuit(): void;
    submitUserCreateNoCheckRelatives(): void;
    createAndAttacheUserToMyClass(): void;
    attachUserToMyClass(): void;
    attachUserToMyClassOnly(): void;
    selectUserToAttach(user: User): void;
    openCreateRelativeFromStudent(student: User): void;
    //from others
    openLightbox(name: string)
    closeLightbox();
    selectedSchoolId(classroom: ClassRoom): string;
    selectedClass: ClassRoom;
}
export async function UserCreateDelegate($scope: UserCreateDelegateScope) {
    // === Private fields
    const onSearchChange = new Subject<string>();
    let classroom: ClassRoom;
    // === Scope fields
    $scope.fields = UserCreateField;
    $scope.userCreate = {
        addAfterSubmit: false,
        searching: false,
        form: new User,
        duplicates: [],
        userToAttach: null,
        search: '',
        select: null,
        results: [],
        submitting: false,
        checkRelations: true,
        minDate: moment().add(-100, "year").toDate(),
        maxDate: moment().toDate()
    }
    // === Private methods
    const checkField = (field: UserCreateField, method: string) => {
        if (typeof field == "undefined") {
            console.warn(`[Directory][UserCreate.${method}] Field should not undefined`)
            return false;
        }
        return true;
    }
    const cleanForm = () => {
        $scope.userCreate.addAfterSubmit = false;
        $scope.userCreate.userToAttach = null;
        $scope.userCreate.form = new User;
        $scope.userCreate.form.type = "Student";
        $scope.userCreate.duplicates = [];
        $scope.userCreate.submitting = false;
        $scope.userCreate.checkRelations = true;
        cleanSearch();
    }
    const cleanSearch = () => {
        $scope.userCreate.search = "";
        $scope.userCreate.results = [];
        $scope.userCreate.select = null;
        $scope.userCreate.searching = true;
    }
    const search = async (query: string) => {
        try {
            if (query && query.length >= 1 && $scope.isUserCreateSearchVisible()) {
                $scope.userCreate.results = [];
                $scope.userCreate.searching = true;
                const founded = await directoryService.searchInDirectory(query, {
                    structures: [$scope.selectedSchoolId($scope.selectedClass)]
                });
                //#24229 we cant use background filter
                const filtered = founded.filter(f => (f as User).profile == "Student");
                $scope.userCreate.results = filtered.map(f => ({
                    innerObject: f as User,
                    profile: "Student" as UserTypes,
                    toString() {
                        return (f as User).displayName;
                    }
                }));
                $scope.safeApply();
            }
        } finally {
            $scope.userCreate.searching = false;
        }
    }
    const createUserAndAttach = async () => {
        const { firstName, lastName, birthDate, type, relatives, email } = $scope.userCreate.form;
        const res = await directoryService.saveUserForClass(classroom.id, {
            birthDate,
            lastName,
            firstName,
            email,
            childrenIds: relatives && relatives.map(c => c.id),
            type
        });
        notify.success("user.added");
        return res;
    }
    const afterSubmit = (created: User) => {
        if ($scope.userCreate.addAfterSubmit) {
            $scope.goToCreateUserForm();
        } else {
            $scope.closeLightbox();
        }
        if (created) {
            $scope.onUserCreated.next([created]);
        }
    }
    // === Add listeners
    (onSearchChange as Observable<string>).distinctUntilChanged().debounceTime(450).subscribe(value => {
        search(value);
    });
    $scope.onClassLoaded.subscribe(c => {
        classroom = c;
    })
    // === Methods
    $scope.goToCreateUserForm = (clean = true) => {
        $scope.userCreate.checkRelations = true;
        if(clean) cleanForm();
        $scope.openLightbox("admin/create-user/form");
    }
    $scope.goToImportUser = () => $scope.openLightbox("admin/create-user/import");
    $scope.isUserCreateRequired = (field) => {
        const type = $scope.userCreate.form.type;
        if (!type) return;
        if (checkField(field, "isUserCreateRequired")) {
            switch (type) {
                case "Student":
                    switch (field) {
                        case UserCreateField.Name:
                        case UserCreateField.FName:
                        case UserCreateField.Birthdate:
                            return true;
                    }
                    break;
                case "Personnel":
                case "Relative":
                case "Teacher":
                    switch (field) {
                        case UserCreateField.Name:
                        case UserCreateField.FName:
                            return true;
                    }
                    break;
            }
        }
        return false;
    }
    $scope.onUserCreateBlur = (field) => {
        if (checkField(field, "onUserCreateBlur")) {
            switch (field) {
                case UserCreateField.Name: {
                    const form = $scope.userCreate.form;
                    if (form && form.type == "Relative") {
                        search(form.lastName);
                    }
                    break;
                }
            }
        }

    }
    $scope.onUserCreateChange = (field) => {
        if (checkField(field, "onUserCreateChange")) {
            switch (field) {
                case UserCreateField.Type:
                    const type = $scope.userCreate.form.type;//reset fields when changing type
                    $scope.userCreate.form = new User;
                    $scope.userCreate.form.type = type;
                    break;
            }
        }
    }
    $scope.onUserCreateSearchChange = function () {
        onSearchChange.next($scope.userCreate.search);
    }
    $scope.isUserCreateSearchVisible = function () {
        const type = $scope.userCreate.form.type;
        return type && type == "Relative";
    }
    $scope.onUserCreateSearchClean = function () {
        cleanSearch();
    }
    $scope.onUserCreateSearchSelect = function () {
        if ($scope.userCreate.select && $scope.userCreate.select.innerObject)
            $scope.userCreate.form.relatives.push($scope.userCreate.select.innerObject)
    }
    $scope.onUserCreateRemoveChild = function (user) {
        $scope.userCreate.form.relatives = $scope.userCreate.form.relatives.filter(u => u !== user);
    }
    $scope.submitUserCreateThenAdd = () => {
        $scope.userCreate.addAfterSubmit = true;
        submitUserCreate();
    }
    $scope.submitUserCreateThenQuit = () => {
        $scope.userCreate.addAfterSubmit = false;
        submitUserCreate();
    }
    $scope.submitUserCreateNoCheckRelatives = () => {
        $scope.userCreate.checkRelations = false; // Show it only once
        submitUserCreate();
    }

    const submitUserCreate = async function () {
        if(await warnNoRelatives()) return;
        if ($scope.userCreate.submitting) return;
        try {
            $scope.userCreate.submitting = true;
            if(await warnOnDuplicates()) return;
            // #47174, Track this event
            $scope.tracker.trackEvent( TRACK.event, TRACK.ACCOUNT_CREATION.action, 
                TRACK.name( $scope.userCreate.addAfterSubmit ? TRACK.ACCOUNT_CREATION.ADD : TRACK.ACCOUNT_CREATION.CREATE, $scope.userCreate.form.type) 
            );
            // === there is no duplicate create the user
            const user = await createUserAndAttach();
            afterSubmit(user);
        } finally {
            $scope.userCreate.submitting = false;
            $scope.userCreate.checkRelations = true;
        }
    }
    const warnNoRelatives = async function () {
        const { type, relatives } = $scope.userCreate.form;
        if ($scope.userCreate.checkRelations && type === "Relative" && relatives.length<1) {
            $scope.goToWarnNoRelatives();
            return true;
        }
        return false;
    }
    const warnOnDuplicates = async function () {
        const { lastName, type } = $scope.userCreate.form;
        if (type === "Student" || type === "Relative") {
            const params = {
                structures: [$scope.selectedSchoolId($scope.selectedClass)]
            };
            // === check wether the user exists
            const founded = await directoryService.searchInDirectory(lastName, params);
            //#24057 we cant use background filter because it does not include students not attached to any classes
            const filtered = founded.filter(f => (f as User).profile == type);
            // === if it exists => display duplicate modal
            if (filtered.length) {
                // there is no more than 2 users most of the time
                await Promise.all((filtered as User[]).map(u => u.open({ withChildren: false })));
                $scope.userCreate.duplicates = filtered as User[];
                $scope.userCreate.userToAttach = $scope.userCreate.duplicates[0];
                $scope.goToDuplicateUserForm();
                return true;
            }
        }
        return false;
    }
    $scope.selectUserToAttach = function (user) {
        if (user) {
            $scope.userCreate.userToAttach = user;
        }
    }
    $scope.goToWarnNoRelatives = () => $scope.openLightbox("admin/create-user/no-relatives");
    $scope.goToDuplicateUserForm = () => $scope.openLightbox("admin/create-user/duplicate");
    $scope.attachUserToMyClass = async function () {
        const user = $scope.userCreate.userToAttach;
        if (!user) {
            console.warn("[Directory][UserCreate.attachUserToMyClass] should select at least one user to attach:", user);
            return;
        }
        // #47174, Track this event (addendum 10-2021)
        $scope.tracker.trackEvent( TRACK.event, TRACK.ACCOUNT_CREATION.action, 
            TRACK.name( TRACK.ACCOUNT_CREATION.ATTACH, $scope.userCreate.form.type) 
        );
        await directoryService.addExistingUserToClass(classroom.id, user, $scope.userCreate.form.relatives);
        afterSubmit(user);
    }
    $scope.attachUserToMyClassOnly = async function () {
        const user = $scope.userCreate.userToAttach;
        if (!user) {
            console.warn("[Directory][UserCreate.attachUserToMyClassOnly] should select at least one user to attach:", user);
            return;
        }
        // #47174, Track this event (addendum 10-2021)
        $scope.tracker.trackEvent( TRACK.event, TRACK.ACCOUNT_CREATION.action, 
            TRACK.name( TRACK.ACCOUNT_CREATION.MOVE, $scope.userCreate.form.type) 
        );
        await directoryService.changeUserClass(user, {
            fromClasses: user.classIds,
            toClass: classroom.id,
            withRelative: true
        });
        afterSubmit(user);
    }
    $scope.createAndAttacheUserToMyClass = async function () {
        // #47174, Track this event (addendum 10-2021)
        $scope.tracker.trackEvent( TRACK.event, TRACK.ACCOUNT_CREATION.action, 
            TRACK.name( TRACK.ACCOUNT_CREATION.DOUBLE, $scope.userCreate.form.type) 
        );
        const user = await createUserAndAttach();
        afterSubmit(user);
    }
    $scope.canUserCreateSubmit = function (form) {
        return form.$valid && !$scope.userCreate.submitting;
    }
    $scope.classnameForDuplicateUser = function (user) {
        const classes: string[] = user.classNames;
        if (classes.length === 0) {
            return lang.translate("classAdmin.duplicate.label.none");
        }
        if (classes.length === 1) {
            return lang.translate("classAdmin.duplicate.label.one").replace("[[className]]", classes[0]);
        }
        // There are at least two classes
        return lang.translate("classAdmin.duplicate.label.several").replace("[[classNames]]", classes.join(", "));
    }
    $scope.isUserAttachSelected = function (user: User) {
        return $scope.userCreate.userToAttach == user;
    }
    $scope.openCreateRelativeFromStudent = function (student) {
        $scope.userCreate.form = new User;
        $scope.userCreate.form.type = "Relative";
        $scope.userCreate.form.relatives = [student];
        $scope.goToCreateUserForm(false);
    }
}
