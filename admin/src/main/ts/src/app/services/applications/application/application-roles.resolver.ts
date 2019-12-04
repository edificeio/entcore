import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, Router} from '@angular/router';

import {globalStore} from '../../../core/store/global.store';
import {RoleModel} from '../../../core/store/models/role.model';
import {ApplicationModel} from '../../../core/store/models/application.model';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { routing } from 'src/app/core/services/routing.service';

@Injectable()
export class ApplicationRolesResolver implements Resolve<RoleModel[] | Boolean> {

    constructor(
        private spinner: SpinnerService,
        private router: Router,
        private ns: NotifyService
    ) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<RoleModel[] | Boolean> {
        const structure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        const appId = route.params.appId;
        const targetApp: ApplicationModel = structure && structure.applications.data.find(a => a.id == appId);

        if (!targetApp) {
            this.router.navigate(['/admin', structure._id, 'services', 'applications']);
        } else {
            return this.spinner.perform('portal-content', targetApp.syncRoles(structure._id)
                .then(() => targetApp.roles)
                .catch(error => {
                    this.ns.error('user.root.error.text', 'user.root.error', error);
                    return false;
                })
            );
        }
    }

}
