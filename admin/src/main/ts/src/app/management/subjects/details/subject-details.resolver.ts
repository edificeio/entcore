import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot} from '@angular/router';

import {routing} from '../../../core/services/routing.service';
import {NotifyService} from '../../../core/services/notify.service';
import {SpinnerService} from 'ngx-ode-ui';
import {globalStore, GlobalStore} from '../../../core/store/global.store';
import {SubjectModel} from '../../../core/store/models/subject.model';

@Injectable()
export class SubjectDetailsResolver implements Resolve<SubjectModel> {

    constructor(
        private spinner: SpinnerService,
        private router: Router,
        private ns: NotifyService,
        private globalStore: GlobalStore
    ) {
    }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): SubjectModel {
        let structure = globalStore.structures.data.find(s => s.id === routing.getParam(route, 'structureId'));
        let subject = structure &&
            structure.subjects.data.find(u => u.id === route.params['subjectId']);

        if (subject) {
            return subject;
        } else {
            this.router.navigate(['/admin', structure.id, 'users', 'filter'], {replaceUrl: false});
        }
    }
}