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
            <span *ngSwitchCase="0">Il n'existe aucun rôle configuré pour cette application</span>
            <span *ngSwitchDefault>Attribuer les droits de {{ app.details.name }}</span>
        </div>
        <div *ngFor="let role of app.roles | orderBy: name">
            <panel-section section-title="{{ role.roleName }}">
                <button (click)="showLightbox = true">
                    <s5l>Ajouter des groupes</s5l>
                    <i class="fa fa-plus"></i>
                </button>
                <div class="flex-container">
                    <div *ngFor="let group of role.groups | filter: filterGroup()" class="flex-item">
                        <label>{{ group.name }}</label>
                        <i class="fa fa-times action" (click)="removeGroupFromRole(group.id, role.id)"></i>
                    </div>
                </div>
            </panel-section>
        </div>
        <light-box class="inner-list" [show]="showLightbox" 
            (onClose)="showAddGroupLightbox = false">
            <div class="panel-header">
                <span>Ajouter des groupes</span>
                <list-component
                    [model]="servicesStore.structure.applications.data"
                    sort="name"
                    [inputFilter]="filterByInput"
                    searchPlaceholder="search.group"
                    noResultsLabel="list.results.no.group"
                    (inputChange)="groupInputFilter = $event">
                    <ng-template let-item>
                        {{ item.name }}
                    </ng-template>
                </list-component>
                <button type="submit" (click)="test = true">
                    <s5l>Enregistrer</s5l>
                </button>
            </div>

        </light-box>
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
        private servicesStore: ServicesStore
    ) {}

    ngOnInit(): void {
        this.routeSubscriber = this.route.params.subscribe(params => {
            if (params['appId']) {
                this.servicesStore.application = this.servicesStore.structure
                    .applications.data.find(a => a.id === params['appId'])
                this.cdRef.markForCheck()
            }
        })
        
        this.appSubscriber = this.route.data.subscribe(data => {
            if(data["details"]) {
                Object.assign(this.servicesStore.application.details, 
                    data['details'])
                this.app = this.servicesStore.application
                this.cdRef.markForCheck()
            }
            if(data["roles"]) {
                Object.assign(this.servicesStore.application.roles,
                    data['roles'].filter(r => r.appId === this.servicesStore.application.id))
                this.app = this.servicesStore.application
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

    removeGroupFromRole(): void {

    }

    filterGroup() {
       
    }
}