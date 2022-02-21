import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import { SpinnerService } from 'ngx-ode-ui';
import { UnlinkedUser, UnlinkedUserService } from 'src/app/admc/search/unlinked/unlinked.service';

@Injectable()
export class AdmcSearchUnlinkedResolver implements Resolve<UnlinkedUser[]> {

    constructor(private spinner: SpinnerService, private unlinkedUserService: UnlinkedUserService) { }

    resolve(route: ActivatedRouteSnapshot): Promise<UnlinkedUser[]> {
        return this.spinner.perform( 
            'portal-content', 
            this.unlinkedUserService.list()
                .then( users => users.filter(u=>!!u.displayName) ) // hide users without a displayName
        );
    }
}
