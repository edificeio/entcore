import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, Router} from '@angular/router';
import { UserModel } from 'src/app/core/store/models/user.model';
import { SpinnerService } from 'ngx-ode-ui';
import { globalStore } from 'src/app/core/store/global.store';
import { routing } from 'src/app/core/services/routing.service';

@Injectable()
export class UserDetailsResolver implements Resolve<UserModel | Error> {

    constructor(private spinner: SpinnerService, private router: Router) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<UserModel> {
        const structure = globalStore.structures.data.find(s => s.id === routing.getParam(route, 'structureId'));
        let user = structure &&
            structure.users.data.find(u => u.id === route.params.userId);
        let removedUser = structure &&
            structure.removedUsers.data.find(u => u.id === route.params.userId);

        if ((user && !removedUser) || (user && removedUser)) {
            return this.spinner.perform('portal-content', user.userDetails.sync()
                .catch((err) => {
                    this.router.navigate(['/admin', structure.id, 'users', 'list'], {replaceUrl: false});
                }).then(() => {
                    return user;
                }));
        } else if(removedUser && !user) {
            return this.spinner.perform('portal-content', removedUser.userDetails.sync()
                .catch((err) => {
                    this.router.navigate(['/admin', structure.id, 'users', 'relink'], {replaceUrl: false});
                }).then(() => {
                    return removedUser;
                }));
        } else
        {
            user = new UserModel();
            user.userDetails.id = routing.getParam(route, 'userId');
            return this.spinner.perform('portal-content', user.userDetails.sync()
                .catch((err) => {
                    this.router.navigate(['/admin', structure.id, 'users', 'list'], {replaceUrl: false});
                }).then(() => {
                    user.id = user.userDetails.id;
                    user.displayName = user.userDetails.displayName;
                    user.structures = user.userDetails.structureNodes.map(s => { return { id: s.id, name: s.name, externalId: s.externalId }});
                    return user;
                }));
            //this.router.navigate(['/admin', structure.id, 'users'], {replaceUrl: false});
        }
    }
}
