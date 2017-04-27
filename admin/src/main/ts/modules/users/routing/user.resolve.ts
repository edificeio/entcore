import { routing } from '../../../routing/routing.utils'
import { Injectable } from '@angular/core'
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router'

import { LoadingService } from '../../../services'
import { globalStore, UserModel } from '../../../store'

@Injectable()
export class UserResolve implements Resolve<UserModel | Error> {

    constructor(private ls: LoadingService, private router: Router){}

    resolve(route: ActivatedRouteSnapshot): Promise<UserModel> {
        let structure = globalStore.structures.data.find(s => s.id === routing.getParam(route, 'structureId'))
        let user = structure &&
            structure.users.data.find(u => u.id === route.params['userId'])

        if(user) {
             return this.ls.perform('users-content', user.userDetails.sync()
                .catch((err) => {
                    this.router.navigate(['/admin', structure.id, 'users'], {replaceUrl: false})
                }).then(() => {
                    return user
                }))
        } else {
            this.router.navigate(['/admin', structure.id, 'users', 'filter'], {replaceUrl: false})
        }
    }
}