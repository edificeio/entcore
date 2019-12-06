import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { GroupModel } from '../../core/store/models/group.model';
import { GroupsStore } from '../groups.store';


@Component({
    selector: 'ode-groups-type-view',
    templateUrl: './groups-type-view.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupsTypeViewComponent extends OdeComponent implements OnInit, OnDestroy {

    groupType: string;
    groupInputFilter: string;
    selectedGroup: GroupModel;

    constructor(
        public groupsStore: GroupsStore,
        injector: Injector) {
            super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.route.params.subscribe(params => {
            this.groupsStore.group = null;
            const type = params.groupType;
            const allowedTypes = ['manualGroup', 'profileGroup', 'functionalGroup', 'functionGroup'];
            if (type && allowedTypes.indexOf(type) >= 0) {
                this.groupType = params.groupType;
                this.changeDetector.markForCheck();
            } else {
                this.router.navigate(['..'], {relativeTo: this.route});
            }
        }));
        this.subscriptions.add(this.groupsStore.$onchange.subscribe(field => {
            if (field === 'structure') {
                this.changeDetector.markForCheck();
                this.changeDetector.detectChanges();
            }
        }));

        // handle change detection from create button click of group-root.component
        this.subscriptions.add(this.route.url.subscribe(path => {
            this.changeDetector.markForCheck();
        }));
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
