import { User, ClassRoom, UserTypes, School } from "../model";
import { EventDelegateScope, TRACK } from "./events";
import { template, notify, idiom, model } from "entcore";
import { directoryService } from '../service';

export interface ChooseClassDelegateScope extends EventDelegateScope {
    chooseClassModel: {
        readonly schools: School[];
        selectedSchoolId?: string;
        readonly classrooms: ClassRoom[];
        selectedClassroomId?: string;
        chosen: ClassRoom[];
        ckCSS: (c:ClassRoom)=> any;
        previous: ()=>Promise<void>;
        next: (step:number)=>Promise<void>;
        listClassrooms: ()=>Promise<ClassRoom[]>;
        toggleChoice: (c:ClassRoom)=>void;
    }
    // from others
    $watch: (s:string, f:()=>void)=>void;
    $apply: (s:string)=>void;
    // selectedClass: ClassRoom;
    // reloadClassroom(classroom: ClassRoom): void;
}
export function ChooseClassDelegate($scope: ChooseClassDelegateScope) {
    // === Private attributes
    let _schools: School[] = [];
    let _classrooms: ClassRoom[] = [];
    // === Public attributes
    const _model = $scope.chooseClassModel = {
        get schools() { return _schools },
        get classrooms() { return _classrooms },
        selectedSchoolId: null,
        selectedClassroomId: null,
        chosen: [],
        ckCSS: (c:ClassRoom) => {
            const isChosen = _model.chosen.findIndex(clazz=>clazz.id===c.id) >= 0;
            return {
                'selected': isChosen
            }
        },
        previous: async () => {
            _model.selectedSchoolId = null;
            _model.selectedClassroomId = null;
            _classrooms = [];
        },
        next: async (step:number) => {
            if( step===1 && _model.selectedSchoolId ) {
                await _model.listClassrooms();
            } else if( step===2 && _model.chosen.length ) {
                await directoryService.addMeToClasses(_model.chosen);
                // We must reload since the whole model has evolved : structures, schools, comm rules...
                window.location.reload();
            }
        },
        listClassrooms: async () => {
            const school = await directoryService.fetchSchool(_model.selectedSchoolId);
            _classrooms = school.classrooms;
            _model.chosen = [];
            $scope.$apply("chooseClassModel.classrooms");
            return _classrooms;
        },
        toggleChoice: (c:ClassRoom) => {
            if( c ) {
                const idx = _model.chosen.findIndex(clazz=>clazz.id===c.id);
                if( idx < 0 ) _model.chosen.push( c );
                else          _model.chosen.splice(idx, 1);
            }
        }
    }
    // === Init listener: listen school changes
    $scope.onSchoolLoaded.subscribe( async loaded => {
        _schools = model.me.structures.map( (s, idx) => {
            return {name: model.me.structureNames[idx], id: model.me.structures[idx]};
        });
        if( _schools && _schools.length===1 && _schools[0] ) {
            _model.selectedSchoolId = _schools[0].id;
            await _model.listClassrooms();
        }
    });
    $scope.$watch( "chooseClassModel.selectedSchoolId", () => {
        //apply
    });
    // === Methods
}