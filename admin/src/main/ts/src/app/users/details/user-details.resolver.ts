import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, Router} from '@angular/router';
import { UserModel } from 'src/app/core/store/models/user.model';
import { SpinnerService } from 'src/app/core/services/spinner.service';
import { globalStore } from 'src/app/core/store/global.store';
import { routing } from 'src/app/core/services/routing.service';

@Injectable()
export class UserDetailsResolver implements Resolve<UserModel | Error> {

    constructor(private spinner: SpinnerService, private router: Router) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<UserModel> {
        const structure = globalStore.structures.data.find(s => s.id === routing.getParam(route, 'structureId'));
        const user = structure &&
            structure.users.data.find(u => u.id === route.params.userId);

        if (user) {
            return this.spinner.perform('portal-content', user.userDetails.sync()
                .catch((err) => {
                    this.router.navigate(['/admin', structure.id, 'users'], {replaceUrl: false});
                }).then(() => {
                    return user;
                }));
        } else {
            this.router.navigate(['/admin', structure.id, 'users', 'filter'], {replaceUrl: false});
        }
    }
}
