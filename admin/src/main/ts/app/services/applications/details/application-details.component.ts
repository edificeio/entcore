import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,
    ChangeDetectorRef, Input } from "@angular/core"
import { ActivatedRoute, Router,  Data } from '@angular/router'

import { Subscription } from 'rxjs/Subscription'
import { SpinnerService, routing } from '../../../core/services'
import { globalStore as globalStore } from '../../../core/store'

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
            <panel-section section-title="{{ role.roleName }}">
                <button (click)="showLightbox = true">
                    {{ 'add.groups' | translate }}
                    <i class="fa fa-plus"></i>
                </button>
                <div class="flex-container">
                    <div *ngFor="let group of role.groups" class="flex-item">
                        <label>{{ group.name }}</label>
                        <i class="fa fa-times action" (click)="removeGroupFromRole(group.id, role.id)"></i>
                    </div>
                </div>
            </panel-section>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApplicationDetailsComponent  implements OnInit, OnDestroy {
    
    app: ApplicationModel
    showAddGroupLightbox: boolean = false
    private appSubscriber: Subscription
    private routeSubscriber: Subscription

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private ls: SpinnerService,
        public servicesStore: ServicesStore
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

    addGroupToRole(): void {

    }

    removeGroupFromRole(groupId: string, roleId: string): void {
        /*let role = this.servicesStore.application.roles
            .find(role => role.id == roleId)
        let groupIndex: number 
        role.groups.find((group, index) => {
            if (group.id == groupId){
                groupIndex = index
                return true
            }
            else
                return false
        })
        role.groups.splice(groupIndex, 1)*/
    }

    filterGroup() {
       
    }
}