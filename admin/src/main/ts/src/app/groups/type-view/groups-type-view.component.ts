import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';

import {GroupsStore} from '../groups.store';
import {GroupModel} from '../../core/store/models';

@Component({
    selector: 'ode-groups-type-view',
    templateUrl: './groups-type-view.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupsTypeViewComponent implements OnInit, OnDestroy {

    groupType: string;
    groupInputFilter: string;
    selectedGroup: GroupModel;

    private typeSubscriber: Subscription;
    private dataSubscriber: Subscription;
    private urlSubscriber: Subscription;

    constructor(
        public groupsStore: GroupsStore,
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef) {
    }

    ngOnInit() {
        this.typeSubscriber = this.route.params.subscribe(params => {
            this.groupsStore.group = null;
            const type = params.groupType;
            const allowedTypes = ['manualGroup', 'profileGroup', 'functionalGroup', 'functionGroup'];
            if (type && allowedTypes.indexOf(type) >= 0) {
                this.groupType = params.groupType;
                this.cdRef.markForCheck();
            } else {
                this.router.navigate(['..'], {relativeTo: this.route});
            }
        });
        this.dataSubscriber = this.groupsStore.$onchange.subscribe(field => {
            if (field === 'structure') {
                this.cdRef.markForCheck();
                this.cdRef.detectChanges();
            }
        });

        // handle change detection from create button click of group-root.component
        this.urlSubscriber = this.route.url.subscribe(path => {
            this.cdRef.markForCheck();
        });
    }

    ngOnDestroy() {
        this.dataSubscriber.unsubscribe();
        this.typeSubscriber.unsubscribe();
        this.urlSubscriber.unsubscribe();
    }

    isSelected = (group: GroupModel) => {
        return this.selectedGroup && group && this.selectedGroup.id === group.id;
    }

    filterByInput = (group: GroupModel) => {
        if (!this.groupInputFilter) { return true; }
        return group.name.toLowerCase()
            .indexOf(this.groupInputFilter.toLowerCase()) >= 0;
    }

    showCompanion(): boolean {
        const groupTypeRoute = '/admin/' +
            (this.groupsStore.structure ? this.groupsStore.structure.id : '') +
            '/groups/' + this.groupType;

        let res: boolean = this.router.isActive(groupTypeRoute + '/create', true);
        if (this.groupsStore.group) {
            res = res || this.router.isActive(groupTypeRoute + '/' + this.groupsStore.group.id + '/details', true)
                || this.router.isActive(groupTypeRoute + '/' + this.groupsStore.group.id + '/communication', true);
        }
        return res;
    }

    closePanel() {
        this.router.navigateByUrl('/admin/' + (this.groupsStore.structure ? this.groupsStore.structure.id : '') +
            '/groups/' + this.groupType);
    }

    routeToGroup(g: GroupModel) {
        this.router.navigate([g.id, 'details'], {relativeTo: this.route});
    }
}
