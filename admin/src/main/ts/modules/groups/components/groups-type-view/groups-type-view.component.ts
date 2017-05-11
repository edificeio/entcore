import { Subscription } from 'rxjs/Subscription'
import { GroupModel } from '../../../../store/models'
import { ActivatedRoute, Router } from '@angular/router'
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core'
import { GroupsStore } from '../../store'
import { LoadingService } from '../../../../services'

@Component({
    selector: 'groups-type-view',
    template: `
        <side-layout (closeCompanion)="closePanel()" [showCompanion]="showCompanion()">
            <div side-card>
                <list-component
                    [model]="groupsStore.structure?.groups.data"
                    [filters]="{type: groupType}"
                    [inputFilter]="filterByInput"
                    sort="name"
                    searchPlaceholder="search.group"
                    [isSelected]="isSelected"
                    [display]="display"
                    (inputChange)="groupInputFilter = $event"
                    (onSelect)="routeToGroup($event)">
                </list-component>
            </div>
            <div side-companion>
                <router-outlet></router-outlet>
            </div>
        </side-layout>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupsTypeView implements OnInit, OnDestroy {

    private groupType : string
    private typeSubscriber : Subscription
    private dataSubscriber : Subscription
    private urlSubscriber: Subscription

    constructor(
        private groupsStore: GroupsStore,
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private ls: LoadingService) {}

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

    // List component  properties
    private groupInputFilter : string
    private isSelected = (group: GroupModel) => {
        return this.groupsStore.group === group
    }
    private filterByInput = (group: GroupModel) => {
        if(!this.groupInputFilter) return true
        return group.name.toLowerCase()
            .indexOf(this.groupInputFilter.toLowerCase()) >= 0
    }
    private display = (group: GroupModel) => { return group.name }

    // Routing
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

    private closePanel() {
        this.router.navigateByUrl('/admin/' + (this.groupsStore.structure ? this.groupsStore.structure.id : '') +
            '/groups/' + this.groupType)
    }
    private routeToGroup(g:GroupModel) {
        this.router.navigate([g.id], { relativeTo: this.route })
    }
}