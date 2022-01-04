import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';

import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { routing } from 'src/app/core/services/routing.service';
import { ApplicationModel } from 'src/app/core/store/models/application.model';
import { RoleActionModel } from 'src/app/core/store/models/role.model';
import { g_appStore } from '../../../admc-apps.store';
import { AdmcAppsRolesService } from '../../admc-apps-roles.service';

@Injectable()
export class ActionsResolver implements Resolve<Array<RoleActionModel> | boolean> {

    constructor(
        private spinner: SpinnerService,
        private ns: NotifyService,
        private roleSvc:AdmcAppsRolesService
    ) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<Array<RoleActionModel> | boolean> {
        const app:ApplicationModel = g_appStore.applications.data.find(
            s => s.id === routing.getParam(route, 'appId'));
        if (app) {
            return this.spinner.perform('portal-content', this.roleSvc.getApps()
            .then( apps => apps.find(ar=>ar.id===app.id) )
            .then( appTemplate => appTemplate.actions )
            .catch(error => {
                this.ns.error('user.root.error.text', 'user.root.error', error);
                return false;
            }));
        } else {
            return Promise.resolve(false);
        }
    }

}
