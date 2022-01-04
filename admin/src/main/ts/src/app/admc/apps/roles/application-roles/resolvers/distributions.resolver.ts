import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';

import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { AdmcAppsRolesService } from '../../admc-apps-roles.service';

@Injectable()
export class DistributionsResolver implements Resolve<Array<string> | boolean> {

    constructor(
        private spinner: SpinnerService,
        private ns: NotifyService,
        private roleSvc:AdmcAppsRolesService
    ) {
    }
    
    resolve(route: ActivatedRouteSnapshot): Promise<Array<string> | boolean> {
        return this.spinner.perform('portal-content', this.roleSvc.getDistributionTemplates()
            .catch(error => {
                this.ns.error('user.root.error.text', 'user.root.error', error);
                return false;
            }));
    }

}
