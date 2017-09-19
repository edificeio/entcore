import { Injectable } from '@angular/core'
import { ActivatedRoute, ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router'

import { globalStore, ApplicationCollection } from '../../../core/store'
import { ApplicationModel, ApplicationDetailsModel } from '../../../core/store/models'
import { SpinnerService, routing } from '../../../core/services'

@Injectable()
export class ApplicationDetailsResolver implements Resolve<ApplicationDetailsModel|boolean> {

    constructor(private spinner: SpinnerService, private router: Router) { }
    
        resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<ApplicationDetailsModel|boolean> {            
            let structure = globalStore.structures.data.find(
                s => s.id === routing.getParam(route, 'structureId'))
            let appId = route.params["appId"]
            let targetApp = structure && structure.applications.data.find(a => a.id === appId)

            if (!targetApp) {
                return this.router.navigate(["/admin", structure._id, "services"])
            }
            return this.spinner.perform('portal-content', targetApp.details.sync()
                .then(app => {
                    return app.data
                })
                .catch(err => {
                    console.error(err)
                    return this.router.navigate(["/admin", structure._id, "services/applications"])
                }))
        }
}