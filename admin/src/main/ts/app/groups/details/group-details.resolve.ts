import { Injectable } from '@angular/core'
import { ActivatedRoute, ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router'

import { SpinnerService, routing } from '../../core/services'
import { globalStore, GroupModel } from '../../core/store'

@Injectable()
export class GroupDetailsResolve implements Resolve<boolean> {

    constructor(private ls: SpinnerService, private router: Router) { }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
        let structure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'))
        let groupType = routing.getParam(route, "groupType")
        let groupId = route.params["groupId"]
        let targetGroup = structure && structure.groups.data.find(g => g.id === groupId)

        if (!targetGroup) {
            return this.router.navigate(["/admin", structure._id, "groups", groupType])
        }
        if (targetGroup.users && targetGroup.users.length < 1) {
            this.ls.perform('groups-content', targetGroup.syncUsers()
                .catch(err => {
                    console.error(err)
                    return this.router.navigate(["/admin", structure._id, "groups", groupType])
                }))
        }
        return
    }
}