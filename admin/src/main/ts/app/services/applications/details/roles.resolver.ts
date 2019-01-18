import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';

import { globalStore } from '../../../core/store';
import { RoleModel } from '../../../core/store/models/role.model';
import { ApplicationModel } from '../../../core/store/models/application.model';
import { NotifyService, routing, SpinnerService } from '../../../core/services';

import { ServicesStore } from '../../services.store';

@Injectable()
export class ApplicationRolesResolver implements Resolve<RoleModel[] | Boolean> {

    constructor(
        private spinner: SpinnerService,
        private router: Router,
        private servicesStore: ServicesStore,
        private ns: NotifyService
    ) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<RoleModel[] | Boolean> {
        let structure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        let appId = route.params['appId'];
        let targetApp: ApplicationModel = structure && structure.applications.data.find(a => a.id == appId);

        if (!targetApp) {
            this.router.navigate(['/admin', structure._id, 'services', 'applications']);
        } else {
            return this.spinner.perform('portal-content', targetApp.syncRoles(structure._id)
                .then(() => targetApp.roles)
                .catch(error => {
                    this.ns.error('user.root.error.text', 'user.root.error', error);
                    return false;
                })
            )
        }
    }

}