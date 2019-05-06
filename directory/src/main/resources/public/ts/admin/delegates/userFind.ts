import { EventDelegateScope } from "./events";
import { User, UserTypes, Mood, ClassRoom, School } from "../model";
import { notify, model } from "entcore";
import { directoryService } from "../service";
const uniqEs6 = (arrArg) => {
    return arrArg.filter((elem, pos, arr) => {
        return arr.indexOf(elem) == pos;
    });
}
export interface UserFindDelegateScope extends EventDelegateScope {
    userFindModel: {
        students: User[];
        relatives: User[];
        teachers: User[];
        personnels: User[];
        guests: User[];
        selectedStudents: UserSelection[];
        selectedRelatives: UserSelection[];
        selectedTeachers: UserSelection[];
        selectedPersonnels: UserSelection[];
        selectedGuests: UserSelection[];
        //
        users: User[];
        usersSearched: User[];
        classrooms: ClassRoom[];
        selectedClassId: string;
        selectedClassName: string;
        typefilter: { [type in UserTypes]: { selected: boolean, className: string } };
        search: string;
        selectedUser: User[]
        countSelectedUsers: number
        selectedUserHavingClass: UserSelection[]
        selectedUsersWithRelatives: User[]
    }
    userFindSelect(user: User): void;
    userFindUnSelect(user: User): void;
    userFindSelectAll(): void;
    userFindUnSelectAll(): void;
    userFindCanSubmit(): boolean;
    goToFindUserForm(): void;
    isUserTypeSelected(type: UserTypes): boolean;
    userFindSubmit(): void
    userFindChangeClass(): void
    userFindOnlyAddToClass(): void
    userFindBack(): void
    // from others
    openLightbox(name: string): void
    closeLightbox(): void;
}
type UserSelection = { user: User, relatives: { [key: string]: User }, classId: string[], classNames: string[] };
export async function UserFindDelegate($scope: UserFindDelegateScope) {
    // === Private attributs
    let _lockSubmit = false;
    let _classroom: ClassRoom;
    let _classrooms: ClassRoom[];
    let _selectedClassroom: ClassRoom;
    let _schools: School[] = null;
    let _selectedUsers: UserSelection[] = [];
    let _typefilter: { [type in UserTypes]: { selected: boolean, className: string } } = {
        "Student": { selected: true, className: "student" },
        "Relative": { selected: false, className: "parent" },
        "Teacher": { selected: true, className: "teacher" },
        "Personnel": { selected: true, className: "staff" },
        "Guest": { selected: true, className: "guest" }
    };
    let _search: string = "";
    // === Public attributes
    $scope.userFindModel = {
        users: [],
        students: [],
        personnels: [],
        relatives: [],
        teachers: [],
        guests: [],
        usersSearched: [],
        get classrooms() { return _classrooms },
        get selectedClassName() {
            return _selectedClassroom ? _selectedClassroom.name : ""
        },
        get selectedClassId() {
            return _selectedClassroom ? _selectedClassroom.id : "-1"
        },
        set selectedClassId(id: string) {
            const clazz = id == "-1" ? null : _classrooms.find(c => c.id == id);
            selectClassRoom(clazz);
        },
        get typefilter() { return _typefilter },
        get search() { return _search },
        set search(q: string) { _search = q; updateSearchUsers() },
        get selectedUser() { return _selectedUsers.map(u => u.user); },
        get countSelectedUsers() { return _selectedUsers.length },
        get selectedStudents() { return _selectedUsers.filter(u => u.user.profile == "Student") },
        get selectedRelatives() { return _selectedUsers.filter(u => u.user.profile == "Relative") },
        get selectedTeachers() { return _selectedUsers.filter(u => u.user.profile == "Teacher") },
        get selectedPersonnels() { return _selectedUsers.filter(u => u.user.profile == "Personnel") },
        get selectedGuests() { return _selectedUsers.filter(u => u.user.profile == "Guest") },
        get selectedUserHavingClass() {
            return _selectedUsers.filter(u => u.classId.length > 0);
        },
        get selectedUsersWithRelatives() {
            return selectionsToFlatUsers(_selectedUsers);
        }
    }
    //=== Listeners
    $scope.onClassLoaded.subscribe(async (s) => {
        _classroom = s;
        let school = directoryService.schoolOfClassroom(
            await getSchools(),
            _classroom
        );
        let school2 = await directoryService.fetchSchool(school.id);
        _classrooms = school2.classrooms.filter(c=>_classroom.id!=c.id);
        const first = _classrooms[0] || null;
        selectClassRoom(first);
    })
    //$scope.onSchoolLoaded.subscribe(loaded => {
    // _schools = loaded;
    // _classrooms = _schools.map(sc => sc.classrooms).reduce((a1, a2) => a1.concat(a2), [])
    //});
    //=== Private methods
    const getSchools = async () => {
        if (_schools == null) {
            _schools = await directoryService.getSchoolsForUser(model.me.userId);
        }
        return _schools;
    }
    const selectionToFlatUsers = (selection: UserSelection) => {
        const relatives: User[] = selection.relatives ? Object.values(selection.relatives) : []
        return [selection.user, ...relatives];
    }
    const selectionsToFlatUsers = (selection: UserSelection[]) => {
        return selection.map(s => selectionToFlatUsers(s)).reduce((a1, a2) => a1.concat(a2), [])
    }
    const setUsers = (users: User[], isTemporary: boolean = false) => {
        if (!isTemporary) $scope.userFindModel.users = users;
        $scope.userFindModel.students = users.filter(u => u.profile == "Student" || u.type == "Student");
        $scope.userFindModel.relatives = users.filter(u => u.profile == "Relative" || u.type == "Relative");
        $scope.userFindModel.personnels = users.filter(u => u.profile == "Personnel" || u.type == "Personnel");
        $scope.userFindModel.teachers = users.filter(u => u.profile == "Teacher" || u.type == "Teacher");
        $scope.userFindModel.guests = users.filter(u => u.profile == "Guest" || u.type == "Guest");
    }
    const selectClassRoom = async (c: ClassRoom) => {
        _selectedClassroom = c;
        if (_selectedClassroom) {
            setUsers(await directoryService.findVisible(_selectedClassroom.id))
        } else {
            setUsers(await directoryService.getDetachedUsers(model.me.structures))
        }
        $scope.safeApply();
    }
    const someSelectedUserHasClass = () => {
        return $scope.userFindModel.selectedUserHavingClass.length > 0;
    }
    const submitAddUser = async () => {
        if (_lockSubmit) return;
        try {
            _lockSubmit = true;
            const users = _selectedUsers.map(u => u.user);
            await directoryService.linkUsersToClass(users, {
                withRelative: true,
                toClass: _classroom.id
            })
            notify.info("classAdmin.userFind.success")
            $scope.closeLightbox();
        } catch (e) {
            notify.info("classAdmin.userFind.error")
        } finally {
            _lockSubmit = false;
            $scope.queryClassRefresh.next(null);
        }
    }
    const submitChangeClass = async () => {
        if (_lockSubmit) return;
        try {
            _lockSubmit = true;
            const users = _selectedUsers.map(u => u.user);
            const classIds = _selectedUsers.map(u => u.classId);
            await directoryService.changeUsersClass(users, classIds, {
                withRelative: true,
                toClass: _selectedClassroom.id
            });
            notify.info("classAdmin.userFind.success")
            $scope.closeLightbox();
        } catch (e) {
            notify.info("classAdmin.userFind.error")
        } finally {
            _lockSubmit = false;
            $scope.queryClassRefresh.next(null);
        }
    }
    // === Methods
    $scope.goToFindUserForm = () => {
        cleanForm();
        $scope.openLightbox("admin/find-users/main");
    }
    const cleanForm = () => {
        _selectedUsers = [];
        _lockSubmit = false;
        _search = "";
        updateSearchUsers();
    }
    const updateSearchUsers = () => {
        //console.log("searchUsers()", $scope.userFindModel.search);
        const filteredBySearch = directoryService.findUsers($scope.userFindModel.search, $scope.userFindModel.users);
        const selectedIds = $scope.userFindModel.selectedUsersWithRelatives.map(u => u.id);
        const filteredBySearchAndSelected = filteredBySearch.filter(u => {
            return selectedIds.indexOf(u.id) == -1;
        })
        //console.log("filteredBySearch", filteredBySearchAndSelected);
        setUsers(filteredBySearchAndSelected, true);
    }
    $scope.isUserTypeSelected = (type) => $scope.userFindModel.typefilter[type].selected;
    $scope.userFindSelect = (user: User) => {
        //add current user
        const clazzName = $scope.userFindModel.selectedClassName;
        const clazzId = $scope.userFindModel.selectedClassId;
        const hasClazzId = clazzId && clazzId != "-1";
        const selection: UserSelection = { user, relatives: {}, classId: hasClazzId ? [clazzId] : [], classNames: hasClazzId ? [clazzName] : [] };
        _selectedUsers.push(selection);
        //add his parents if needed
        if (user.relativeList) {
            for (let u of user.relativeList) {
                //relatives
                const relative = new User({ id: u.relatedId, displayName: u.relatedName, profile: "Relative" });
                selection.relatives[u.relatedId] = relative;
            }
        }
        updateSearchUsers();
        $scope.safeApply();
    }
    $scope.userFindUnSelect = (user: User) => {
        //remove current user => auto remove his parents
        _selectedUsers = _selectedUsers.filter(u => u.user.id != user.id);
        updateSearchUsers();
        $scope.safeApply();
    }
    $scope.userFindSelectAll = () => {
        if (_typefilter.Student.selected)
            $scope.userFindModel.students.forEach(s => $scope.userFindSelect(s));
        if (_typefilter.Relative.selected)
            $scope.userFindModel.relatives.forEach(s => $scope.userFindSelect(s));
        if (_typefilter.Personnel.selected)
            $scope.userFindModel.personnels.forEach(s => $scope.userFindSelect(s));
        if (_typefilter.Teacher.selected)
            $scope.userFindModel.teachers.forEach(s => $scope.userFindSelect(s));
        if (_typefilter.Guest.selected)
            $scope.userFindModel.guests.forEach(s => $scope.userFindSelect(s));
    };
    $scope.userFindUnSelectAll = () => {
        _selectedUsers.forEach(s => $scope.userFindUnSelect(s.user));
    };
    $scope.userFindCanSubmit = () => _selectedUsers.length > 0;
    $scope.userFindSubmit = () => {
        if (_lockSubmit) return;
        if (someSelectedUserHasClass()) {
            $scope.openLightbox("admin/find-users/confirm")
        } else {
            submitAddUser();
        }
    }
    $scope.userFindBack = () => {
        $scope.openLightbox("admin/find-users/main")
    }
    $scope.userFindChangeClass = () => {
        submitChangeClass();
    }
    $scope.userFindOnlyAddToClass = () => {
        submitAddUser();
    }
}