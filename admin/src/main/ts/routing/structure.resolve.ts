import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot } from '@angular/router'

import { StructureModel, globalStore } from '../store'
import { LoadingService } from '../services'

@Injectable()
export class StructureResolve implements Resolve<StructureModel> {

    constructor(private ls: LoadingService){}

    resolve(route: ActivatedRouteSnapshot): Promise<StructureModel> {
        let structure: StructureModel = globalStore.structures.data.find(s => s.id === route.params['structureId'])
        if(!structure) {
            return new Promise((res, rej) => {
                rej('structure.not.found')
            })
        }

        return this.ls.perform('portal', this.sync(structure))
    }

    private sync(structure: StructureModel): Promise<StructureModel> {
        structure.syncClasses()
        structure.syncGroups()
        structure.syncSources()
        structure.syncAafFunctions()
        return Promise.resolve(structure)
    }
}