import { _, idiom as lang } from 'entcore';
import { EventDelegateScope } from "./events";
import { ClassRoom, Network, PersonApiResult, School } from '../model';
import { directoryService } from '../service';
import { Observable, Subject } from 'rxjs';


export interface MenuDelegateScope extends EventDelegateScope {
    selectClassroom(classroom: ClassRoom): void;
    reloadClassroom(classroom: ClassRoom): void;
    selectedSchoolName(classroom: ClassRoom): string;
    selectedSchoolId(classroom: ClassRoom): string;
    openClassList($event: any): void;
    closeClassList(): void;
    saveClassInfos(): void;
    belongsToMultipleSchools(): boolean;
    hasSelectedClass(): boolean;
    canOpenCreateUserModal(): boolean;
    openCreateUserModal();
    listOpened: boolean;
    selectedClass: ClassRoom;
    // from others
    classrooms: ClassRoom[];
    openLightbox(name: string)

}

export function MenuDelegate($scope: MenuDelegateScope) {
    // === Private methods
    let lastSelectedId = null;
    let myClasses = [];
    let myClassePromise = ($scope.onSchoolLoaded as Observable<School[]>)//
        .flatMap(schools => schools.map(sc => sc.classrooms))//
        .toPromise()
    const setSelectedClassById = async function (classroomId: string, savePref: boolean, forceReload: boolean = false) {
        console.log("[Directory][Menu.setSelectedClassById] selecting a classroom: ", classroomId, savePref)
        if (classroomId && ((lastSelectedId != classroomId) || forceReload)) {
            savePref && directoryService.savePreference({ selectedClassId: classroomId })
            const fetched = await directoryService.fetchClassById(classroomId, { withUsers: true });
            $scope.selectedClass = fetched;
            $scope.onClassLoaded.next(fetched);
            $scope.safeApply()
        } else {
            console.warn("[Directory][Menu.setSelectedClassById] trying to select an undefined classroom: ", classroomId);
            $scope.selectedClass = null;
        }
        lastSelectedId = classroomId;
        $scope.safeApply()
    }
    const setSelectedClass = async function (classroom: ClassRoom, savePref: boolean) {
        $scope.selectedClass = classroom;
        classroom && setSelectedClassById(classroom.id, savePref);
    }
    const getPreferenceClassId = async function (): Promise<string> {
        const val = await directoryService.getPreference();
        return val && val.selectedClassId;
    }
    const refreshClass = async () => {
        if (!$scope.selectedClass) return;
        const fetched = await directoryService.fetchClassById($scope.selectedClass.id, { withUsers: true });
        $scope.selectedClass = fetched;
        $scope.onClassRefreshed.next(fetched);
    }
    const selectedSchool = function (classroom: ClassRoom) {
        return classroom && schools.find(sc => !!sc.classrooms.find(clazz => clazz.id == classroom.id));
    }
    // === Init attributes
    $scope.classrooms = [];
    let schools: School[] = [];
    // === Init listener : listen network changes to load my class
    $scope.queryClassRefresh.subscribe(clazz => {
        refreshClass();
        $scope.safeApply();
    });
    $scope.onSchoolLoaded.subscribe(async loaded => {
        schools = loaded;
        myClasses = schools.map(sc => sc.classrooms).reduce((a1, a2) => a1.concat(a2), [])
        $scope.classrooms = myClasses;
        let myClassId = await getPreferenceClassId();
        //
        if (myClassId) {
            setSelectedClassById(myClassId, false);
        } else if ($scope.classrooms.length > 0 && !$scope.hasSelectedClass()) {
            setSelectedClass($scope.classrooms[0], false);
        }
        $scope.safeApply();
    });
    $scope.onUserCreated.subscribe(users => {
        refreshClass();
        $scope.safeApply();
    })
    // === Methods
    $scope.selectedSchoolName = function (classroom) {
        const school = selectedSchool(classroom);
        return school && school.name;
    }
    $scope.selectedSchoolId = function (classroom) {
        const school = selectedSchool(classroom);
        return school && school.id;
    }
    $scope.selectClassroom = function (classroom) {
        setSelectedClass(classroom, true);
        $scope.listOpened = false;
    }
    $scope.reloadClassroom = function (classroom) {
        classroom && setSelectedClassById(classroom.id, false, true);
    }
    $scope.openClassList = function ($event) {
        $event.stopPropagation();
        $scope.listOpened = !$scope.listOpened;
    }
    $scope.closeClassList = function () {
        $scope.listOpened = false;
    }
    $scope.saveClassInfos = function () {
        directoryService.saveClassInfos($scope.selectedClass);
    }

    $scope.belongsToMultipleSchools = function () {
        return schools.length > 1;
    }
    $scope.hasSelectedClass = function () {
        return !!$scope.selectedClass;
    }
    $scope.canOpenCreateUserModal = () => $scope.classrooms.length > 0;
    $scope.openCreateUserModal = () => $scope.openLightbox('admin/create-user/menu');
}