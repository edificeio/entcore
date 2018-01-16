import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot } from '@angular/router'

import { globalStore, GroupCollection } from '../core/store'
import { GroupModel } from '../core/store/models'
import { SpinnerService, routing } from '../core/services'

@Injectable()
export class GroupsResolver implements Resolve<GroupModel[]> {

    constructor(private spinner: SpinnerService) { }

    resolve(route: ActivatedRouteSnapshot): Promise<GroupModel[]> {
        let currentStructure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'))
        if (currentStructure.groups.data.length > 0) {
            return Promise.resolve(currentStructure.groups.data)
        } else {
            return this.spinner.perform('portal-content', currentStructure.groups.sync()
                .then(() => {
                    return currentStructure.groups.data
                }).catch(e => {
                    console.error(e)
                }))
        }
    }

}