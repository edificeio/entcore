import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot } from '@angular/router'
import { globalStore, GroupCollection } from '../../../store'
import { GroupModel } from '../../../store/models'
import { LoadingService } from '../../../services'
import { routing } from '../../../routing/routing.utils'

@Injectable()
export class GroupsResolve implements Resolve<GroupModel[]> {

    constructor(private ls: LoadingService) { }

    resolve(route: ActivatedRouteSnapshot): Promise<GroupModel[]> {
        let currentStructure = globalStore.structures.data.find(s => s.id === routing.getParam(route, 'structureId'))
        if (currentStructure.groups.data.length > 0) {
            return Promise.resolve(currentStructure.groups.data)
        } else {
            return this.ls.perform('portal-content', currentStructure.groups.sync()
                .then(() => {
                    return currentStructure.groups.data
                }).catch(e => {
                    console.error(e)
                }))
        }
    }

}