import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, Router} from '@angular/router';

import {g_appStore} from '../../admc-apps.store';
import {RoleModel} from 'src/app/core/store/models/role.model';
import {ApplicationModel} from 'src/app/core/store/models/application.model';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { routing } from 'src/app/core/services/routing.service';

@Injectable()
export class ApplicationRolesResolver implements Resolve<ApplicationModel | Boolean> {

    constructor(
        private spinner: SpinnerService,
        private router: Router,
        private ns: NotifyService
    ) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<ApplicationModel | Boolean> {
        const app:ApplicationModel = g_appStore.applications.data.find(
            s => s.id === routing.getParam(route, 'appId'));
        if (!app) {
            this.router.navigate(['/admin', 'roles']);
        } else {
            return this.spinner.perform('portal-content', app.syncRoles('0')
                .then(() => app)
                .catch(error => {
                    this.ns.error('user.root.error.text', 'user.root.error', error);
                    return false;
                })
            );
        }
    }

}
