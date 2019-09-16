import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot } from '@angular/router'

import { StructureModel, globalStore } from '../core/store'
import { SpinnerService, ProfilesService } from '../core/services'

@Injectable()
export class StructureResolver implements Resolve<StructureModel> {

    constructor(private spinner: SpinnerService){}

    resolve(route: ActivatedRouteSnapshot): Promise<StructureModel> {
        let structure: StructureModel = globalStore.structures.data.find(s => s.id === route.params['structureId'])
        if(!structure) {
            return new Promise((res, rej) => {
                rej('structure.not.found')
            })
        }

        return this.spinner.perform('portal-content', sync(structure))
    }

}

export function sync(structure: StructureModel, force?: boolean): Promise<StructureModel> {
    let classesPromise = structure.syncClasses(force);
    let groupsPromise = structure.syncGroups(force);
    let sourcesPromise = structure.syncSources(force);
    let aafFunctionsPromise = structure.syncAafFunctions(force);
    let profilesPromise = ProfilesService.getProfiles().then(p => structure.profiles = p);
    return Promise.all<any>([classesPromise, groupsPromise, sourcesPromise, aafFunctionsPromise, profilesPromise])
        .then(() => Promise.resolve(structure));
}
