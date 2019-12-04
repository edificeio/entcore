import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {routing} from '../core/services/routing.service';

import {SpinnerService} from 'ngx-ode-ui';
import {sync} from '../structure/structure.resolver';
import { UserModel } from '../core/store/models/user.model';
import { StructureModel } from '../core/store/models/structure.model';
import { globalStore } from '../core/store/global.store';

@Injectable()
export class UsersResolver implements Resolve<UserModel[]> {

    constructor(private spinner: SpinnerService) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<UserModel[]> {
        const currentStructure: StructureModel = globalStore.structures.data.find(s => s.id === routing.getParam(route, 'structureId'));

        if (route.queryParams.sync) {
            sync(currentStructure, true);
        }

        if (currentStructure.users.data.length > 0 && !route.queryParams.sync) {
            return Promise.resolve(currentStructure.users.data);
        } else {
            const p = new Promise<UserModel[]>( (resolve, reject) => {
                currentStructure.users.sync()
                .then(() => {
                    resolve(currentStructure.users.data);
                }).catch(e => {
                    reject(e);
                });
            });
            return this.spinner.perform('portal-content', p);
        }
    }
}
