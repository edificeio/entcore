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
                <button (click)="filterByType('all')">{{ 'all' | translate }}</button>
                <button (click)="filterByType('profile')">{{ 'applications.groups.structure' | translate }}</button>
                <button (click)="filterByType('class')">{{ 'applications.classes' | translate }}</button>
                <button (click)="filterByType('functional')">{{ 'applications.groups.functional' | translate }}</button>
                <button (click)="filterByType('manual')">{{ 'applications.groups.manual' | translate }}</button>
            </div>
            <form>
                <list-component
                [model]="this.groupsList"
                sort="name"
                [inputFilter]="filterByName"
                searchPlaceholder="search.group"
                noResultsLabel="list.results.no.groups"
                (inputChange)="this.groupInputFilter = $event">
                    <ng-template let-item>
                        <span>
                            <input type="checkbox" id="{{ item.id }}" />
                            <label>{{ item.name }}</label>                        
                        </span>
                        </ng-template>
                </list-component>
                <button type="submit" (click)="addGroupsToRole()">{{ 'save' | translate }}</button>
            </form>
        </light-box>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApplicationDetailsComponent  implements OnInit, OnDestroy {
    
    app: ApplicationModel
    selectedRole: RoleModel
    showLightbox: boolean = false
    groupInputFilter: string
    groupsList: {}[]
    
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

        this.filterByType('all')
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
        if (type == 'all'){
            this.groupsList = this.servicesStore.structure.groups.data
            this.groupsList = this.groupsList.concat(this.servicesStore.structure.classes)
        }
        else if (type == 'class')
            this.groupsList = this.servicesStore.structure.classes
        else if (type == 'profile')
            this.groupsList = this.servicesStore.structure.groups.data.filter(g => g.type == 'ProfileGroup' && g.subType == 'StructureGroup')
        else if (type == 'functional')
            this.groupsList = this.servicesStore.structure.groups.data.filter(g => g.type == 'FunctionalGroup')
        else if (type == 'manual')
            this.groupsList = this.servicesStore.structure.groups.data.filter(g => g.type == 'ManualGroup')
        this.cdRef.markForCheck()
    }

    addGroupsToRole(): void {
        let roleId = this.selectedRole.id
        let groupsIds = this.getCheckedGroups()
        

    }

    private getCheckedGroups() {

        let arr = []
        let elmts = document.querySelectorAll('input[type=checkbox]:checked')

        for (let i = 0; i < elmts.length; ++i)
            arr.push(elmts[i].id)

        return arr
    }

    removeGroupFromRole(groupId: string, roleId: string): void {
        let role = this.servicesStore.application.roles.find(role => role.id == roleId)
        role.removeGroupLink(groupId, roleId)
            .then(() => {
                this.cdRef.markForCheck()
            })
    }
}