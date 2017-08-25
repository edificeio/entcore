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

        return this.spinner.perform('portal', this.sync(structure))
    }

    private sync(structure: StructureModel): Promise<StructureModel> {
        structure.syncClasses()
        structure.syncGroups()
        structure.syncSources()
        structure.syncAafFunctions()
        ProfilesService.getProfiles().then(p => structure.profiles = p)
        return Promise.resolve(structure)
    }
}