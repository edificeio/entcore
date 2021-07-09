import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, Resolve } from "@angular/router";
import { BundlesService } from "ngx-ode-sijil";
import { SpinnerService } from "ngx-ode-ui";
import { routing } from "src/app/core/services/routing.service";
import { globalStore } from "src/app/core/store/global.store";
import { WidgetModel } from "src/app/core/store/models/widget.model";

@Injectable()
export class WidgetsResolver implements Resolve<WidgetModel[]> {

    constructor(private spinner: SpinnerService, private bundlesService: BundlesService) { }

    resolve(route: ActivatedRouteSnapshot): Promise<WidgetModel[]> {
        const currentStructure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        if (currentStructure.widgets && 
            currentStructure.widgets.data && 
            currentStructure.widgets.data.length > 0) {
            return Promise.resolve(currentStructure.widgets.data);
        } 
        const p = new Promise<WidgetModel[]>((resolve, reject) => {
            currentStructure.widgets.syncWidgets()
                .then(data => {
                    resolve(currentStructure.widgets.data);
                }, error => {
                    reject(error);
                });
        });
        return this.spinner.perform('portal-content', p);
    }
}