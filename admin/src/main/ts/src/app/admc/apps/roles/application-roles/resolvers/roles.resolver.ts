import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';

import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { Role, AdmcAppsRolesService } from '../../admc-apps-roles.service';

@Injectable()
export class RoleActionsResolver implements Resolve<Array<Role> | boolean> {

    constructor(
        private spinner: SpinnerService,
        private ns: NotifyService,
        private roleSvc:AdmcAppsRolesService
    ) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<Array<Role> | boolean> {
        return this.spinner.perform('portal-content', this.roleSvc.getRoles()
            .catch(error => {
                this.ns.error('user.root.error.text', 'user.root.error', error);
                return false;
            }));
    }

}
