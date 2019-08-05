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

        return this.spinner.perform('portal', sync(structure))
    }
}

export function sync(structure: StructureModel, force?: boolean): Promise<StructureModel> {
    structure.syncClasses(force)
    structure.syncGroups(force)
    structure.syncSources(force)
    structure.syncAafFunctions(force)
    ProfilesService.getProfiles().then(p => structure.profiles = p)
    return Promise.resolve(structure)
}