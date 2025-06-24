import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';

import { StructureModel } from '../core/store/models/structure.model';
import { globalStore } from '../core/store/global.store';
import { SpinnerService } from 'ngx-ode-ui';
import { ProfilesService } from '../core/services/profiles.service';

@Injectable()
export class StructureResolver implements Resolve<StructureModel> {

    constructor(private spinner: SpinnerService) {}

    resolve(route: ActivatedRouteSnapshot): Promise<StructureModel> {
        const structure: StructureModel = globalStore.structures.data.find(s => s.id === route.params.structureId);
        if (!structure) {
            return new Promise((res, rej) => {
                rej('structure.not.found');
            });
        }

        return this.spinner.perform('portal-content', sync(structure));
    }

}

export function sync(structure: StructureModel, force?: boolean): Promise<StructureModel> {
    return Promise.all<any>([
        structure.syncClasses(force), 
        structure.syncGroups(force), 
        structure.syncSources(force), 
        structure.syncAafFunctions(force), 
        ProfilesService.getProfiles().then(p => structure.profiles = p), 
        structure.syncPositions(force), 
        /* COCO-3782 this sync is too eager for high-level structures. 
         * Instead, it is synced where required => in BroadcastGroup tab for now.
        structure.syncLevels(force),
        */
    ]).then(() => Promise.resolve(structure));
}
