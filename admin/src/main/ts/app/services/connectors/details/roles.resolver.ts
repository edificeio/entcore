import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router';

import { globalStore} from '../../../core/store';
import { RoleModel } from '../../../core/store/models';
import { SpinnerService, routing, NotifyService } from '../../../core/services';

import { ServicesStore } from '../../services.store';

@Injectable()
export class ConnectorRolesResolver implements Resolve<RoleModel[]|Boolean> {

    constructor(
        private spinner: SpinnerService, 
        private router: Router,
        private servicesStore: ServicesStore,
        private ns: NotifyService
    ) { }

    resolve(route: ActivatedRouteSnapshot): Promise<RoleModel[]|Boolean> {
        let structure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        let connectorId = route.params["connectorId"];
        let targetConnector = structure && structure.connectors.data.find(a => a.id === connectorId);

        if (!targetConnector) {
            this.router.navigate(["/admin", structure._id, "services", "connectors"]);
        }
        else {
            return this.spinner.perform('portal-content', targetConnector.syncRoles(structure._id, connectorId)
                .then(() => targetConnector.roles)
                .catch(error => {
                    this.ns.error("user.root.error.text", "user.root.error", error);
                    return false
                })
            )
        }
    }

}