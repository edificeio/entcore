import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router'

import { globalStore, GroupCollection } from '../../../core/store'
import { RoleModel } from '../../../core/store/models'
import { SpinnerService, routing } from '../../../core/services'

@Injectable()
export class RolesResolver implements Resolve<RoleModel[]|Boolean> {

    constructor(private spinner: SpinnerService, private router: Router) { }

    resolve(route: ActivatedRouteSnapshot): Promise<RoleModel[]|Boolean> {
        let structureId = routing.getParam(route, 'structureId').toString()
        let structure = globalStore.structures.data.find(
            s => s.id === structureId)
        let appId = route.params["appId"]
        let targetApp = structure && structure.applications.data.find(a => a.id === appId)

        if (!targetApp) {
            return this.router.navigate(["/admin", structure._id, "services"])
        }
        else {
            return this.spinner.perform('portal-content', targetApp.syncRoles(structureId, appId)
                .then(() => targetApp.roles)
                .catch(e => {
                    console.error(e)
                    return this.router.navigate(["/admin", structure._id, "services/applications"])
                })
            )
        }
    }

}