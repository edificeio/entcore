import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,
    ChangeDetectorRef, Input } from "@angular/core"

import { ActivatedRoute, Router,  Data } from '@angular/router'

import { Subscription } from 'rxjs/Subscription'
import { SpinnerService, routing } from '../../../core/services'
import { globalStore as globalStore } from '../../../core/store'

import { ServicesStore } from '../../services.store'
import { ApplicationModel } from '../../../core/store/models'

@Component({
    selector: 'app-details',
    template: `
        <div class="panel-header">
            <span>Attribuer les droits de {{ app.applicationDetails.name }}</span>
        </div>
        <div *ngFor="let action of app.applicationActions.actions">
            <panel-section section-title="{{ action[1] || 'Il n\\'y a pas de droits applicatifs disponibles
                    pour cette application' }}">
                <button *ngIf="action[1]" (click)="showAddGroupLightbox = true">
                    <s5l>Ajouter des groupes</s5l>
                </button>
            </panel-section>
        </div>
        <light-box class="inner-list" [show]="showAddGroupLightbox" 
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
            if(data["appDetails"]) {
                Object.assign(this.servicesStore.application.applicationDetails, data['appDetails'])
                //this.servicesStore.application.applicationDetails = data['appDetails']
                this.app = this.servicesStore.application
                this.cdRef.markForCheck()
            }
            if(data["appActions"]) {
                Object.assign(this.servicesStore.application.applicationActions,
                    data['appActions'].find(a => a.id === this.servicesStore.application.id))
                // this.servicesStore.application.actions = data['appActions']
                //     .find(a => a.id === this.servicesStore.application.id).actions
                this.app = this.servicesStore.application
                this.cdRef.markForCheck()
            }
        })
    }

    ngOnDestroy(): void {
        this.appSubscriber.unsubscribe()
    }

    addGroupToRole(): void {

    }
}