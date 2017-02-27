import { routing } from '../../../routing/routing.utils'
import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot } from '@angular/router'

import { structureCollection, UserModel } from '../../../store'
import { LoadingService } from '../../../services'

@Injectable()
export class UsersResolve implements Resolve<UserModel[]> {

    constructor(private ls: LoadingService){}

    resolve(route: ActivatedRouteSnapshot): Promise<void | UserModel[]> {
        let currentStructure = structureCollection.data.find(s => s.id === routing.getParam(route, 'structureId'))
        if(currentStructure.users.data.length > 0) {
            return Promise.resolve(currentStructure.users.data)
        } else {
            return this.ls.perform('portal-content', currentStructure.users.sync()
                .then(() => {
                    return currentStructure.users.data
                }).catch(e => {
                    console.error(e)
                }))
        }
    }
}