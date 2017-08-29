import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,
    ChangeDetectorRef } from "@angular/core"

import { ActivatedRoute, Router,  Data } from '@angular/router'

import { Subscription } from 'rxjs/Subscription'
import { SpinnerService, routing } from '../../../core/services'

import { ServicesStore } from '../../services.store'

@Component({
    selector: 'apps-details-list',
    template: `
        <side-layout [showCompanion]="true">
            <div side-card>
                <list-component
                    [model]="servicesStore.structure.applications.data"
                    sort="name"
                    [inputFilter]="filterByInput"
                    searchPlaceholder="search.application"
                    noResultsLabel="list.results.no.applications"
                    [isSelected]="isSelected"
                    (inputChange)="appInputFilter = $event"
                    (onSelect)="routeToApplication($event)">
                    <ng-template let-item>
                        {{ item.name }}
                    </ng-template>
                </list-component>
            </div>
            <div side-companion>
                <router-outlet></router-outlet>
            </div>
        </side-layout>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApplicationsDetailsListComponent  implements OnInit, OnDestroy {
    
    appInputFilter: string
    private appsSubscriber: Subscription

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private spinner: SpinnerService,
        private servicesStore: ServicesStore
    ) {}

    ngOnInit(): void {
        this.appsSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['apps']) {
                this.servicesStore.structure.applications.data = data['apps']
                this.cdRef.markForCheck()
            }
        })
    }

    ngOnDestroy(): void {
        this.appsSubscriber.unsubscribe()
    }

    isSelected = (app) => {
        return this.servicesStore.application === app
    }

    routeToApplication(app) {
        this.spinner.perform('portal-content',this.router.navigate(['..', app.id], { relativeTo: this.route }))
    }

    filterByInput = (app: any) => {
        if(!this.appInputFilter) return true
        return app.name.toLowerCase()
            .indexOf(this.appInputFilter.toLowerCase()) >= 0
    }
}