import { AppActionModel } from './../../../core/store/models/applications/application-action.model';
import { AppActionsCollection } from './../../../core/store/collections/app-actions.collection';
import { Injectable } from '@angular/core'
import { ActivatedRoute, ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router'

import { globalStore, ApplicationCollection } from '../../../core/store'
import { ApplicationModel, ApplicationDetailsModel } from '../../../core/store/models'
import { SpinnerService, routing } from '../../../core/services'

@Injectable()
export class ApplicationActionsResolver implements Resolve<AppActionModel[]|boolean> {

    constructor(private spinner: SpinnerService, private router: Router) { }
    
        resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<AppActionModel[]|boolean> {            
            let currentStructure = globalStore.structures.data.find(
                s => s.id === routing.getParam(route, 'structureId'))
            if (currentStructure.appActions.data.length > 0) {
                return Promise.resolve(currentStructure.appActions.data)
            } else {
                return this.spinner.perform('portal-content', currentStructure.appActions.sync()
                    .then(data => {
                        return currentStructure.appActions.data
                    }).catch(e => {
                        console.error(e)
                        return this.router.navigate(["/admin", currentStructure._id, "services/applications"])
                    }))
            }
        }
}