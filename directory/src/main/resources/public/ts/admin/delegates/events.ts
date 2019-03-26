import { Subject } from "rxjs";
import { ClassRoom, Network, User, PersonApiResult, School } from "../model";

export interface EventDelegateScope {
    //Class events
    queryClassRefresh: Subject<ClassRoom>;
    onClassLoaded: Subject<ClassRoom>;
    onClassRefreshed: Subject<ClassRoom>;
    //Network events
    onSchoolLoaded: Subject<School[]>;
    //User events 
    onSelectionChanged: Subject<User[]>;
    onUserCreated: Subject<User[]>;
    onUserUpdate: Subject<User>;
    safeApply(fn?)
}
export function EventDelegate($scope: EventDelegateScope) {
    $scope.queryClassRefresh = new Subject();
    $scope.onClassLoaded = new Subject();
    $scope.onClassRefreshed = new Subject();
    $scope.onSchoolLoaded = new Subject();
    $scope.onSelectionChanged = new Subject();
    $scope.onUserCreated = new Subject();
    $scope.onUserUpdate = new Subject;
    $scope.safeApply = function (fn) {
        const phase = this.$root.$$phase;
        if (phase == '$apply' || phase == '$digest') {
            if (fn && (typeof (fn) === 'function')) {
                fn();
            }
        } else {
            this.$apply(fn);
        }
    };
}