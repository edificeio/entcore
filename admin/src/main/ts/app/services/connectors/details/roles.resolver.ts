import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router';

import { GroupCollection } from '../../../core/store';
import { RoleModel } from '../../../core/store/models';
import { SpinnerService, routing } from '../../../core/services';

import { ServicesStore } from '../../services.store';

@Injectable()
export class ConnectorRolesResolver implements Resolve<RoleModel[]|Boolean> {

    constructor(
        private spinner: SpinnerService, 
        private router: Router,
        private servicesStore: ServicesStore
    ) { }

    resolve(route: ActivatedRouteSnapshot): Promise<RoleModel[]|Boolean> {
        let structureId = routing.getParam(route, 'structureId').toString();
        let structure = this.servicesStore.structure;
        let connectorId = route.params["connectorId"];
        let targetConnector = structure && structure.connectors.data.find(a => a.id === connectorId);

        if (!targetConnector) {
            return this.router.navigate(["/admin", structure._id, "services"]);
        }
        else {
            return this.spinner.perform('portal-content', targetConnector.syncRoles(structureId, connectorId)
                .then(() => targetConnector.roles)
                .catch(e => {
                    console.error(e);
                    return this.router.navigate(["/admin", structure._id, "services/connectors"]);
                })
            )
        }
    }

}