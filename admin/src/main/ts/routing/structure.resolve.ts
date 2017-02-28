import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot } from '@angular/router'

import { StructureModel, globalStore } from '../store'
import { LoadingService } from '../services'

@Injectable()
export class StructureResolve implements Resolve<StructureModel> {

    constructor(private ls: LoadingService){}

    resolve(route: ActivatedRouteSnapshot): Promise<StructureModel> {
        let target = globalStore.structures.data.find(s => s.id === route.params['structureId'])
        if(!target){
            return new Promise((res, rej) => {
                rej('structure.not.found')
            })
        }

        return this.ls.perform('portal-content', target.syncClasses()
            .catch(err =>{
                console.error(err)
            }).then(() => {
                return Promise.resolve(target)
            }))
    }
}