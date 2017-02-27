import { structureCollection } from '../../../store'
import { LoadingService } from '../../../services'
import { routing } from '../../../routing/routing.utils'
import { ActivatedRoute, ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router'
import { Injectable } from '@angular/core'

@Injectable()
export class GroupResolve implements Resolve<void> {

    constructor(private ls: LoadingService, private router : Router){}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<void | boolean> {
        let structure = structureCollection.data.find(s => s.id === routing.getParam(route, 'structureId'))
        let groupType = routing.getParam(route, "groupType")
        let groupId = route.params["groupId"]
        let targetGroup = structure && structure.groups.data.find(g => g.id === groupId)

        if(!targetGroup) {
            this.router.navigate(["/admin", structure._id, "groups", groupType])
            return
        }

        return this.ls.perform('groups-content', <Promise<void>>targetGroup.syncUsers()
            .catch(err => {
                console.error(err)
                this.router.navigate(["/admin", structure._id, "groups", groupType])
            }))
    }
}