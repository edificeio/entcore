import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';

import {globalStore} from '../core/store';
import {SubjectModel} from '../core/store/models';
import {routing, SpinnerService} from '../core/services';

@Injectable()
export class SubjectsResolver implements Resolve<SubjectModel[]> {

    constructor(private spinner: SpinnerService) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<SubjectModel[]> {
        let currentStructure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId')
        );
        if (currentStructure.subjects.data.length > 0) {
            return Promise.resolve(currentStructure.subjects.data)
        } else {
            return this.spinner.perform('portal-content', currentStructure.subjects.sync()
                .then(() => {
                    return currentStructure.subjects.data;
                }).catch(e => {
                    console.error(e);
                }));
        }
    }
}