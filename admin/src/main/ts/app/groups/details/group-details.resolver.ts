import { Injectable } from '@angular/core'
import { ActivatedRoute, ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router'

import { SpinnerService, routing, NotifyService } from '../../core/services'
import { globalStore, GroupModel } from '../../core/store'

@Injectable()
export class GroupDetailsResolver implements Resolve<boolean> {

    constructor(
        private spinner: SpinnerService,
        private router: Router,
        private ns: NotifyService
    ) { }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
        let structure = globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'))
        let groupType = routing.getParam(route, "groupType")
        let groupId = route.params["groupId"]
        let targetGroup = structure && structure.groups.data.find(g => g.id === groupId)

        if (!targetGroup) {
            this.router.navigate(["/admin", structure._id, "groups", groupType])
        }
        if (targetGroup.users && targetGroup.users.length < 1) {
            this.spinner.perform('groups-content', targetGroup.syncUsers()
                .catch(error => {
                    this.ns.error("user.root.error.text", "user.root.error", error);
                    return false
                }))
        }
        return
    }
}