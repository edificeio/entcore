import { routing } from '../core/services/routing.service'
import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot } from '@angular/router'

import { globalStore, UserModel } from '../core/store'
import { SpinnerService } from '../core/services'
import { UsersStore } from './users.store'

@Injectable()
export class UsersResolver implements Resolve<UserModel[]> {

    constructor(private spinner: SpinnerService){}

    resolve(route: ActivatedRouteSnapshot): Promise<UserModel[]> {
        let currentStructure = globalStore.structures.data.find(s => s.id === routing.getParam(route, 'structureId'))

        if(currentStructure.users.data.length > 0) {
            return Promise.resolve(currentStructure.users.data)
        } 
        else {
            return this.spinner.perform('portal-content', currentStructure.users.sync()
                .then(() => {
                    console.log(currentStructure.users.data)
                    return currentStructure.users.data
                }).catch(e => {
                    console.error(e)
                }))
        }
    }
}