import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';

import {globalStore} from '../core/store/global.store';
import {GroupModel} from '../core/store/models/group.model';
import {routing} from '../core/services/routing.service';
import {SpinnerService} from 'ngx-ode-ui';

@Injectable()
export class GroupsResolver implements Resolve<GroupModel[]> {

    constructor(private spinner: SpinnerService) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<GroupModel[]> {
        const currentStructure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId')
        );
        
        if (currentStructure.groups.data.length > 0) {
            return Promise.resolve(currentStructure.groups.data);
        } else {
            const p = new Promise<GroupModel[]> (
                (resolve, reject) => {
                    currentStructure.groups.sync()
                .then(() => {
                    resolve(currentStructure.groups.data);
                }, error => {
                    reject(error);
                });
                }
            );
            return this.spinner.perform('portal-content', p);
        }
    }
}
