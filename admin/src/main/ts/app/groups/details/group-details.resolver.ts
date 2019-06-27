import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Router, RouterStateSnapshot } from '@angular/router';

import { NotifyService, routing, SpinnerService } from '../../core/services';
import { GlobalStore, GroupModel } from '../../core/store';

@Injectable()
export class GroupDetailsResolver implements Resolve<GroupModel> {

    constructor(
        private spinner: SpinnerService,
        private router: Router,
        private ns: NotifyService,
        private globalStore: GlobalStore
    ) {
    }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<GroupModel> {
        const structure = this.globalStore.structures.data.find(
            s => s.id === routing.getParam(route, 'structureId'));
        const groupType = routing.getParam(route, 'groupType');
        const groupId = route.params['groupId'];
        const targetGroup = structure && structure.groups.data.find(g => g.id === groupId);

        if (!targetGroup) {
            this.router.navigate(['/admin', structure._id, 'groups', groupType]);
        }
        if (targetGroup.users && targetGroup.users.length < 1) {
            this.spinner.perform('groups-content', targetGroup.syncUsers()
                .then(() => {
                    return targetGroup;
                })
                .catch(error => {
                    this.ns.error('user.root.error.text', 'user.root.error', error);
                    return targetGroup;
                }));
        }
        return Promise.resolve<GroupModel>(targetGroup);
    }
}
