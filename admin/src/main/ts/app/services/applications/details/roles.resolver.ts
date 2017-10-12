import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router';

import { GroupCollection } from '../../../core/store';
import { RoleModel } from '../../../core/store/models/role.model';
import { ApplicationModel } from '../../../core/store/models/application.model';
import { SpinnerService, routing } from '../../../core/services';

import { ServicesStore } from '../../services.store';

@Injectable()
export class ApplicationRolesResolver implements Resolve<RoleModel[]|Boolean> {

    constructor(
        private spinner: SpinnerService, 
        private router: Router,
        private servicesStore: ServicesStore
    ) { }

    resolve(route: ActivatedRouteSnapshot): Promise<RoleModel[]|Boolean> {
        let structureId = routing.getParam(route, 'structureId').toString();
        let structure = this.servicesStore.structure;
        let appId = route.params["appId"];
        let targetApp:ApplicationModel = structure && structure.applications.data.find(a => a.id == appId);

        if (!targetApp) {
            return this.router.navigate(["/admin", structure._id, "services"]);
        }
        else {
            return this.spinner.perform('portal-content', targetApp.syncRoles(structureId)
                .then(() => targetApp.roles)
                .catch(e => {
                    console.error(e);
                    return this.router.navigate(["/admin", structure._id, "services/applications"]);
                })
            )
        }
    }

}