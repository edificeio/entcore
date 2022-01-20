import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { routing } from 'src/app/core/services/routing.service';
import { globalStore } from 'src/app/core/store/global.store';
import { RoleModel } from 'src/app/core/store/models/role.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { WidgetModel } from 'src/app/core/store/models/widget.model';
import { BundlesService } from "ngx-ode-sijil";

@Injectable()
export class WidgetRolesResolver implements Resolve<Array<RoleModel>> {

    constructor(
        private spinner: SpinnerService,
        private router: Router,
        private ns: NotifyService,
        private bundlesService: BundlesService
    ) { }

    resolve(route: ActivatedRouteSnapshot): Promise<Array<RoleModel>> {
        const structure: StructureModel = globalStore.structures.data.find(s => s.id === routing.getParam(route, 'structureId'));
        const widgetId: string = route.params['widgetId'];
        const targetWidget: WidgetModel = structure && structure.widgets.data.find(a => a.id === widgetId);

        if (!targetWidget) {
            this.router.navigate(['/admin', structure._id, 'services', 'widgets']);
        } else {
            return this.spinner.perform(
                'portal-content', 
                targetWidget.syncRoles(structure._id)
                    .then(() => {
                        targetWidget.roles.forEach(role => {
                            role.name = `${this.bundlesService.translate('services.widget.roles.access')} ${this.bundlesService.translate(targetWidget.name)}`
                            if (role.groups && role.groups.length === 1 && !role.groups[0].id) {
                                role.groups = [];
                            }
                        });
                        return targetWidget.roles
                    })
                    .catch(error => {
                        this.ns.error('user.root.error.text', 'user.root.error', error);
                        return Promise.reject(new Error('error retrieving widget roles: ' + error));
                    })
            );
        }
    }
}