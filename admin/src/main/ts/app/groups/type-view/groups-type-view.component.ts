import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'

import { GroupsStore } from '../groups.store'
import { GroupModel } from '../../core/store/models'
import { SpinnerService } from '../../core/services'

@Component({
    selector: 'groups-type-view',
    template: `
        <side-layout (closeCompanion)="closePanel()" [showCompanion]="showCompanion()">
            <div side-card>
                <list
                    [model]="groupsStore.structure?.groups.data"
                    [filters]="{type: groupType}"
                    [inputFilter]="filterByInput"
                    sort="name"
                    searchPlaceholder="search.group"
                    noResultsLabel="list.results.no.groups"
                    [isSelected]="isSelected"
                    (inputChange)="groupInputFilter = $event"
                    (onSelect)="routeToGroup($event)">
                    <ng-template let-item>
                        {{ item.name }}
                    </ng-template>
                </list>
            </div>
            <div side-companion>
                <router-outlet></router-outlet>
            </div>
        </side-layout>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupsTypeView implements OnInit, OnDestroy {

    groupType : string

    groupInputFilter : string

    private typeSubscriber : Subscription
    private dataSubscriber : Subscription
    private urlSubscriber: Subscription

    constructor(
        public groupsStore: GroupsStore,
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private ls: SpinnerService) {}

    ngOnInit() {
        this.typeSubscriber = this.route.params.subscribe(params => {
            this.groupsStore.group = null
            let type = params["groupType"]
            let allowedTypes = ["manual", "profile", "functional"]
            if(type && allowedTypes.indexOf(type) >= 0) {
                this.groupType = params["groupType"]
                this.cdRef.markForCheck()
            } else {
                this.router.navigate([".."], { relativeTo: this.route })
            }
        })
        this.dataSubscriber = this.groupsStore.onchange.subscribe(field => {
            if(field === "structure"){
                this.cdRef.markForCheck()
                this.cdRef.detectChanges()
            }
        })

        // handle change detection from create button click of group-root.component
        this.urlSubscriber = this.route.url.subscribe(path => {
            this.cdRef.markForCheck()
        })
    }

    ngOnDestroy() {
        this.dataSubscriber.unsubscribe()
        this.typeSubscriber.unsubscribe()
        this.urlSubscriber.unsubscribe()
    }

    isSelected = (group: GroupModel) => {
        return this.groupsStore.group === group
    }

    filterByInput = (group: GroupModel) => {
        if(!this.groupInputFilter) return true
        return group.name.toLowerCase()
            .indexOf(this.groupInputFilter.toLowerCase()) >= 0
    }

    showCompanion(): boolean {
        const groupTypeRoute = '/admin/' + 
            (this.groupsStore.structure ? this.groupsStore.structure.id : '') + 
            '/groups/' + this.groupType

        let res: boolean = this.router.isActive(groupTypeRoute + '/create', true)
        if (this.groupsStore.group) {
            res = res || this.router.isActive(groupTypeRoute + '/' + this.groupsStore.group.id, true)
        }

        return res
    }

    closePanel() {
        this.router.navigateByUrl('/admin/' + (this.groupsStore.structure ? this.groupsStore.structure.id : '') +
            '/groups/' + this.groupType)
    }

    routeToGroup(g:GroupModel) {
        this.router.navigate([g.id], { relativeTo: this.route })
    }
}