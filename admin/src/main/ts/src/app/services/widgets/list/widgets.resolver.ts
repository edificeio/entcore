import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { WidgetModel } from "src/app/core/store/models/widget.model";
import { globalStore } from 'src/app/core/store/global.store';
import { SpinnerService } from "ngx-ode-ui";
import { routing } from "src/app/core/services/routing.service";
import { StructureModel } from "src/app/core/store/models/structure.model";

@Injectable()
export class WidgetsResolver implements Resolve<Array<WidgetModel>> {
    
    constructor(private spinner: SpinnerService) { }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<Array<WidgetModel>> {
        const currentStructure: StructureModel = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        const widgetsListPromise = new Promise<Array<WidgetModel>>((resolve, reject) => {
            currentStructure.widgets.syncWidgets().
                then(data => {
                    resolve(currentStructure.widgets.data);
                }, error => {
                    reject(error);
                });
        });
        return this.spinner.perform('portal-content', widgetsListPromise);
    }
}