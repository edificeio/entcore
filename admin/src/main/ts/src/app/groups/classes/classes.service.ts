import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {tap} from 'rxjs/operators';
import {GroupsStore} from '../groups.store';
import { ClassModel } from 'src/app/core/store/models/structure.model';

export type ClassCreatePayload = {
    name?: string;
};

export type ClassUpdatePayload = {
    name?: string;
};

@Injectable()
export class ClassesService {

    constructor(private httpClient: HttpClient,
                public groupsStore: GroupsStore) {
    }

    public delete(schoolId: string, classId: string): Observable<void> {
        return this.httpClient.delete<void>(`/directory/class/${classId}`)
        .pipe(
            tap(() => {
                this.groupsStore.structure.classes.splice(
                    this.groupsStore.structure.classes.findIndex(c => c.id === classId)
                    , 1);
                if( this.groupsStore.class && this.groupsStore.class.id === classId ) {
                    this.groupsStore.class = null;
                }
            })
        );
    }

    public update(classId: string, classUpdatePayload: ClassUpdatePayload): Observable<void> {
        return this.httpClient.put<void>(`/directory/class/${classId}`, classUpdatePayload)
        .pipe(
            tap(() => {
                const sClass: ClassModel = this.groupsStore.structure.classes.find(c => c.id === classId);
                if (sClass) {
                    Object.assign(sClass, classUpdatePayload);
                }
                Object.assign(this.groupsStore.class, classUpdatePayload);
            })
        );
    }

    public create(schoolId: string, classCreatePayload: ClassCreatePayload, wDefaultRoles:boolean = false): Observable<ClassModel> {
        return this.httpClient.post<ClassModel>(`/directory/class/${schoolId}${wDefaultRoles ? "?setDefaultRoles=true" : ""}`
            , classCreatePayload
            , { headers: {"Content-Type": "application/json"} })
        .pipe(
            tap( clazz => {
                clazz.name = classCreatePayload.name;
                this.groupsStore.structure.classes.push( clazz );
                this.groupsStore.class = clazz;
            })
        );
    }
}
