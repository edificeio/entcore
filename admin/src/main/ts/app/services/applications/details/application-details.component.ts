import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,
    ChangeDetectorRef, Input } from "@angular/core"
import { ActivatedRoute, Router,  Data } from '@angular/router'

import { Subscription } from 'rxjs/Subscription'
import { SpinnerService, routing } from '../../../core/services'
import { globalStore } from '../../../core/store'

import { ServicesStore } from '../../services.store'
import { ApplicationModel, RoleModel } from '../../../core/store/models'

@Component({
    selector: 'app-details',
    template: `
        <div class="panel-header" [ngSwitch]="app.roles.length">
            <span *ngSwitchCase="0">{{ 'list.no.role' | translate }}</span>
            <span *ngSwitchDefault>{{ 'application.give.rights' | translate }} {{ app.name }}</span>
        </div>
        <div *ngFor="let role of app.roles">
            <panel-section section-title="{{ role.name }}">
                <button (click)="this.selectedRole = role; this.showLightbox = true">
                    {{ 'add.groups' | translate }}
                    <i class="fa fa-plus"></i>
                </button>
                <div class="flex-container">
                    <div *ngFor="let group of (role.groups | mapToArray)" class="flex-item">
                        <label>{{ group.value }}</label>
                        <i class="fa fa-times action" (click)="removeGroupFromRole(group.key, role.id)"></i>
                    </div>
                </div>
            </panel-section>
        </div>
        <light-box [show]="showLightbox" (onClose)="showLightbox = false"> 
            <h1 class="panel-header">{{ 'add.groups' | translate }}</h1>
            <div class="panel-header-sub">
                <button (click)="filterByType('classes')">Classes</button>
                <button (click)="filterByType('profiles')">Groupes établissement</button>
            </div>
            <list-component
            [model]="this.groupType"
            sort="name"
            [inputFilter]="filterByName"
            searchPlaceholder="search.group"
            noResultsLabel="list.results.no.groups"
            (inputChange)="this.groupInputFilter = $event">
                <ng-template let-item>
                    <span>
                        <input type="checkbox" (click)="addGroupToRole(item.id)" />
                        <label>{{ item.name }}</label>                        
                    </span>
                    </ng-template>
            </list-component>
        </light-box>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApplicationDetailsComponent  implements OnInit, OnDestroy {
    
    app: ApplicationModel
    selectedRole: RoleModel
    showLightbox: boolean = false
    groupInputFilter: string
    groupType: string
    
    private appSubscriber: Subscription
    private routeSubscriber: Subscription

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private ls: SpinnerService,
        public servicesStore: ServicesStore,
    ) {}

    ngOnInit(): void {
        this.routeSubscriber = this.route.params.subscribe(params => {
            if (params['appId']) {
                this.servicesStore.application = this.servicesStore.structure
                    .applications.data.find(a => a.id === params['appId'])
                this.app = this.servicesStore.application
                this.cdRef.markForCheck()
            }
        })
        
        this.appSubscriber = this.route.data.subscribe(data => {
            if(data["roles"]) {
                this.servicesStore.application.roles = data["roles"]
                this.app.roles = this.servicesStore.application.roles
                this.cdRef.markForCheck()
            }
        })
    }

    ngOnDestroy(): void {
        this.routeSubscriber.unsubscribe()
        this.appSubscriber.unsubscribe()
    }

    filterByName = (group: any) => {
        if(!this.groupInputFilter) return true
        return group.name.toLowerCase()
            .indexOf(this.groupInputFilter.toLowerCase()) >= 0
    }

    filterByType(type: string) {
        
    }

    addGroupToRole(groupId: string): void {
        


    }

    removeGroupFromRole(groupId: string, roleId: string): void {
        let role = this.servicesStore.application.roles.find(role => role.id == roleId)
        role.removeGroupLink(groupId, roleId)
            .then(() => {
                this.cdRef.markForCheck()
            })
    }
}