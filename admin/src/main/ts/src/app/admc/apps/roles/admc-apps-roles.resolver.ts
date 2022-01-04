import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import { SpinnerService } from 'ngx-ode-ui';
import {BundlesService} from 'ngx-ode-sijil';

import { g_appStore } from '../admc-apps.store';
import {ApplicationModel} from 'src/app/core/store/models/application.model';

@Injectable()
export class AdmcAppsRolesResolver implements Resolve<ApplicationModel[]> {

    constructor(private spinner: SpinnerService, private bundlesService: BundlesService) { }

    resolve(route: ActivatedRouteSnapshot): Promise<ApplicationModel[]> {
        if (g_appStore.applications.data.length > 0) {
            return Promise.resolve(g_appStore.applications.data);
        } else {
            const p = new Promise<ApplicationModel[]>((resolve, reject) => {
                g_appStore.applications.syncApps("0")   // 0 <=> all structures
                .then( () => {
                    g_appStore.applications.data.forEach(app => app.displayName = this.bundlesService.translate(app.displayName));
                    resolve(g_appStore.applications.data);
                }, error => {
                    reject(error);
                });
            });
            return this.spinner.perform('portal-content', p);
        }
    }
}
