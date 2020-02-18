import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';

import {globalStore} from '../core/store/global.store';
import {SubjectModel} from '../core/store/models/subject.model';
import {routing} from '../core/services/routing.service';
import {SpinnerService} from 'ngx-ode-ui';

@Injectable()
export class SubjectsResolver implements Resolve<SubjectModel[]> {

    constructor(private spinner: SpinnerService) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<SubjectModel[]> {
        const currentStructure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId')
        );
        if (currentStructure.subjects.data.length > 0) {
            return Promise.resolve(currentStructure.subjects.data);
        } else {
            const p = new Promise<SubjectModel[]>(
                (resolve, reject) => {
                    currentStructure.subjects.sync()
                        .then(() => {
                            resolve(currentStructure.subjects.data);
                        }, error => {
                            reject(error);
                        });
                }
            );
            return this.spinner.perform('portal-content', p);
        }
    }
}