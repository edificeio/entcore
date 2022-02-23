import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from "@angular/router";
import { Observable } from "rxjs";
import { WidgetModel } from "src/app/core/store/models/widget.model";
import { globalStore } from 'src/app/core/store/global.store';
import { SpinnerService } from "ngx-ode-ui";
import { routing } from "src/app/core/services/routing.service";
import { StructureModel } from "src/app/core/store/models/structure.model";
import { DefaultBookmarks, WidgetService } from "src/app/core/services/widgets.service";

@Injectable()
export class DefaultBookmarksResolver implements Resolve<DefaultBookmarks> {
    
    constructor(private spinner: SpinnerService, private widgets:WidgetService) { }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<DefaultBookmarks|null> {
        const currentStructure: StructureModel = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        return this.spinner.perform('portal-content', this.widgets.getMyAppsParameters(currentStructure));
    }
}