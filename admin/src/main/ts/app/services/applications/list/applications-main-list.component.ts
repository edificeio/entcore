import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,
    ChangeDetectorRef } from "@angular/core"

import { ActivatedRoute, Router,  Data } from '@angular/router'

import { Subscription } from 'rxjs/Subscription'
import { SpinnerService, routing } from '../../../core/services'

import { ServicesStore } from '../../services.store'
import { ApplicationCollection } from '../../../core/store/collections'

@Component({
    selector: 'apps-main-list',
    template: `
        <div class="icons-view apps">
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
                    <div class="element">{{ item.name }}</div>
                </ng-template>
            </list-component>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApplicationsMainListComponent implements OnInit, OnDestroy {
    
    private appsSubscriber: Subscription
    appInputFilter: string

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private ls: SpinnerService,
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
        this.servicesStore.application = app
        this.router.navigate([app.id], { relativeTo: this.route })
    }

    filterByInput = (app: any) => {
        if(!this.appInputFilter) return true
        return app.name.toLowerCase()
            .indexOf(this.appInputFilter.toLowerCase()) >= 0
    }
}