import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import { SpinnerService } from 'ngx-ode-ui';
import { routing } from 'src/app/core/services/routing.service';
import { UnlinkedUserDetails, UnlinkedUserService } from '../unlinked.service';

@Injectable()
export class UserDetailsResolver implements Resolve<UnlinkedUserDetails> {

    constructor(private spinner: SpinnerService, private svc:UnlinkedUserService) {
    }

    resolve(route: ActivatedRouteSnapshot): Promise<UnlinkedUserDetails> {
        return this.spinner.perform( 'portal-content', this.svc.fetch(routing.getParam(route, 'userId')) );
    }
}
