import { _, idiom as lang, notify } from 'entcore';
import { EventDelegateScope } from "./events";
import { ClassRoom, UserTypes, User, School } from '../model';
import { directoryService, ReportType } from '../service';

enum ExportTypes {
    Simple, Detail, Mail, CSV
}
enum Column {
    Name, Profil
}
export interface ExportDelegateScope extends EventDelegateScope {
    exportTypes: typeof ExportTypes
    exportCol: typeof Column;
    userExport: { title: string, onlySelected: boolean, profiles: UserTypes[], type: ExportTypes };
    userExportInfo(): string
    userExportPrint(): void;
    userExportSend(confirm?: boolean): void;
    goToExport(onlySelected?: boolean): void;
    userExportCancel(): void
    userExportChoice(type: ExportTypes);
    userExportSubmitProfile(): void;
    userExportCanSubmitProfile(): boolean;
    toggleExportSort(column: Column): void;
    isExportSortAsc(column: Column): boolean;
    isExportSortDesc(column: Column): boolean;
    exportSortAsc(column: Column, $e?: MouseEvent): void
    exportSortDesc(column: Column, $e?: MouseEvent): void
    usersWithoutMails(): User[];
    userProfile(u: User): string
    userExportProfilSelected(type: UserTypes): boolean;
    userExportProfilToggle(type: UserTypes): void
    userExportGoBack();
    userExportCanBack(): boolean;
    usersWithMailsExist(): boolean;
    // from others
    classrooms: ClassRoom[];
    openLightbox(name: string): void
    closeLightbox(): void;

}

export function ExportDelegate($scope: ExportDelegateScope) {
    // === Private attributs
    let _classroom: ClassRoom;
    let _selected: User[] = [];
    let _currentSortDir: "asc" | "desc" = "asc";
    let _currentSort: Column = Column.Name;
    let _usersForMailing: User[] = [];
    let _usersWithoutMails: User[] = [];
    let _usersWithMails: User[] = [];
    let _stack: string[] = [];
    let _schools: School[] = [];
    let _sendingMail = false;
    let _printingWithoutMail = false;
    let _printingAll = false;
    // === Private methods
    const resetStack = () => {
        _stack = [];
    }
    const next = (route: string) => {
        $scope.openLightbox(route);
        _stack.push(route);
    }
    const back = () => {
        _stack.pop();
        if (_stack.length == 0) {
            $scope.closeLightbox()
        } else {
            $scope.openLightbox(_stack[_stack.length - 1])
        }
    }
    const sortUsers = (u1: User, u2: User) => {
        // sort by other fields
        const getter = (u: User) => {
            if (!u) return "";
            switch (_currentSort) {
                case Column.Name:
                    return u.safeDisplayName || "";
                case Column.Profil:
                    break;
            }
            return "";
        }
        const value1 = getter(u1);
        const value2 = getter(u2);
        const res = value1.localeCompare(value2);
        if (_currentSortDir == "desc") {
            return -1 * res;
        }
        return res;
    }
    const onSelectedType = () => {
        if ($scope.userExport.type == ExportTypes.Mail) {
            if (_usersWithoutMails.length) {
                next("admin/export/withoutmail");
            } else {
                next("admin/export/sendmail");
            }
        } else {
            printForAllUsers();
            $scope.closeLightbox();
        }
    }
    const selectUsersForMassMailing = () => {
        _usersForMailing = [];
        if ($scope.userExport.onlySelected) {
            _usersForMailing = [..._selected];
        } else {
            _usersForMailing = _classroom.users.filter(u => {
                return ($scope.userExport.profiles.indexOf(u.type) > -1);
            });
        }
        _usersWithoutMails = _usersForMailing.filter(s => !s.safeHasEmail);
        _usersWithMails = _usersForMailing.filter(s => s.safeHasEmail);
    }
    const getSchool = function () {
        const school = _classroom && _schools.find(sc => !!sc.classrooms.find(clazz => clazz.id == _classroom.id));
        if (!school) {
            console.warn("[Directory][UserExport.getSchool] school should not be undefined", _classroom, school)
        }
        return school;
    }
    const sendForUsersWithMail = async () => {
        try {
            if (_sendingMail) return;
            _sendingMail = true;
            //  send mail
            const school = getSchool();
            await directoryService.sendMassMailing({
                ids: _usersWithMails.map(u => u.id),
                structureId: school.id,
                type: "mail"
            })
            notify.success(lang.translate("directory.notify.mailSent"))
        } catch (e) {
            notify.error(lang.translate("classAdmin.mail.failed"))
        } finally {
            _sendingMail = false;
        }
    }
    const printForUsersWithoutMail = async () => {
        try {
            if (_printingWithoutMail) return;
            _printingWithoutMail = true;
            // print user without mail 
            const school = getSchool();
            await directoryService.generateReport({
                ids: _usersWithoutMails.map(u => u.id),
                structureId: school.id,
                type: "simplePdf"
            })
        } catch (e) {
            notify.error(lang.translate("classAdmin.download.failed"))
        } finally {
            _printingWithoutMail = false;
        }
    }
    const printForAllUsers = async () => {
        try {
            if (_printingAll) return;
            _printingAll = true;
            // print all
            let type: ReportType = null;
            switch ($scope.userExport.type) {
                case ExportTypes.CSV:
                    type = "csv";
                    break;
                case ExportTypes.Detail:
                    type = "newPdf";
                    break;
                case ExportTypes.Simple:
                    type = "simplePdf";
                    break;
                case ExportTypes.Mail:
                default:
                    throw "Invalid report type: " + $scope.userExport.type;
            }
            const school = getSchool();
            await directoryService.generateReport({
                ids: _usersForMailing.map(u => u.id),
                structureId: school.id,
                type
            })
        } catch (e) {
            notify.error(lang.translate("classAdmin.download.failed"))
        } finally {
            _printingAll = false;
        }
    }
    // === Init attributes
    $scope.exportCol = Column;
    $scope.exportTypes = ExportTypes;
    $scope.userExport = {
        type: null,
        title: "",
        profiles: [],
        onlySelected: false
    }
    // === Init listener : listen network changes to load my class
    $scope.onSelectionChanged.subscribe((selected) => {
        _selected = selected;
    })
    $scope.onClassLoaded.subscribe((s) => {
        _classroom = s;
    })
    $scope.onSchoolLoaded.subscribe(loaded => {
        _schools = loaded;
    });
    // === Methods
    $scope.goToExport = function (onlySelected = false) {
        resetStack();
        $scope.userExport = {
            type: null,
            title: lang.translate(onlySelected ? "classAdmin.export.title" : "classAdmin.export.create"),
            profiles: [],
            onlySelected
        }
        if (onlySelected) {
            selectUsersForMassMailing();
            next("admin/export/types");
        } else {
            next("admin/export/profiles");
        }
    }
    $scope.userExportInfo = function () {
        if ($scope.userExport.onlySelected) {
            return (lang.translate("classAdmin.export.count") as string).replace("[[count]]", _selected.length + "");
        } else {
            let len = $scope.userExport.profiles.length;
            let info: string = lang.translate(`classAdmin.export.create.profil${len}`);
            let index = 0;
            for (let profile of $scope.userExport.profiles) {
                const key = `directory.${profile}s`.toLowerCase();
                const value = (lang.translate(key) as string).toLowerCase();
                info = info.replace(`[[profile${++index}]]`, value);
            }
            return info;
        }
    }
    $scope.userExportCancel = function () {
        $scope.closeLightbox();
    }
    $scope.userExportChoice = function (type) {
        if (typeof type == "undefined") {
            console.warn("[Directory][UserExport.userExportChoice] type should not be undefined: ", type)
            return;
        }
        $scope.userExport.type = type;
        onSelectedType();
    }
    $scope.userExportCanSubmitProfile = () => {
        return $scope.userExport.profiles.length > 0;
    }
    $scope.userExportSubmitProfile = () => {
        selectUsersForMassMailing();
        next("admin/export/types");
    }
    $scope.userExportPrint = () => {
        printForUsersWithoutMail();
    }
    $scope.userExportSend = (confirm = false) => {
        if (confirm) {
            sendForUsersWithMail();
            $scope.closeLightbox();
        } else {
            next("admin/export/sendmail");
        }
    }
    $scope.toggleExportSort = function (column) {
        if (_currentSort == column) {
            _currentSort = column;
            _currentSortDir = _currentSortDir == "asc" ? "desc" : "asc";
        } else {
            _currentSort = column;
            _currentSortDir = "asc";
        }
        $scope.safeApply();
    }
    $scope.isExportSortAsc = function (column: Column) {
        return _currentSort == column && _currentSortDir == "asc";
    }
    $scope.isExportSortDesc = function (column: Column) {
        return _currentSort == column && _currentSortDir == "desc";
    }
    $scope.exportSortAsc = function (column: Column, e) {
        e && e.stopPropagation();
        _currentSort = column;
        _currentSortDir = "asc";
        $scope.safeApply();
    }
    $scope.exportSortDesc = function (column: Column, e) {
        e && e.stopPropagation();
        _currentSort = column;
        _currentSortDir = "desc";
        $scope.safeApply();
    }
    $scope.usersWithoutMails = function () {
        return _usersWithoutMails.sort(sortUsers);
    }
    $scope.usersWithMailsExist = function () {
        return _usersWithMails.length > 0;
    }
    $scope.userProfile = function (u) {
        return lang.translate(`directory.${u.type}`);
    }
    $scope.userExportProfilSelected = (type) => {
        return $scope.userExport.profiles.findIndex(t => t == type) > -1;
    }
    $scope.userExportProfilToggle = (type) => {
        if ($scope.userExportProfilSelected(type)) {
            $scope.userExport.profiles = $scope.userExport.profiles.filter(t => t != type)
        } else {
            $scope.userExport.profiles.push(type);
        }
    }
    $scope.userExportGoBack = () => back();
    $scope.userExportCanBack = () => _stack.length > 1;
}