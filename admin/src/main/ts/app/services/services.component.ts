import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy,
     OnInit } from '@angular/core'
import { ActivatedRoute, Router, Data, NavigationEnd } from '@angular/router'

import { Subscription } from 'rxjs/Subscription'

import { ServicesStore } from './services.store'
import { SpinnerService, routing } from '../core/services'

@Component({
    selector: 'services-root',
    template: `
        <h1><i class="fa fa-th"></i><s5l>Services</s5l></h1>
        <div class="tabs">
            <button class="tab" *ngFor="let tab of tabs"
                [routerLink]="tab.view"
                routerLinkActive="active">
                {{ tab.label | translate }}
            </button>
        </div>
        <router-outlet></router-outlet>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServicesComponent implements OnInit, OnDestroy {

    // Subscriberts
    private structureSubscriber: Subscription
    private routerSubscriber: Subscription
    
    tabs = [
        { label: "Applications", view: "applications" },
        { label: "Connecteurs", view: "connectors" },
        { label: "Widgets", view: "widgets" }
    ]

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private ls: SpinnerService,
        private servicesStore: ServicesStore) { }

    ngOnInit(): void {
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure']) {
                this.servicesStore.structure = data['structure']
                this.cdRef.markForCheck()
            }
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if(e instanceof NavigationEnd)
                this.cdRef.markForCheck()
        })
    }

    ngOnDestroy(): void {
        this.structureSubscriber.unsubscribe()
        this.routerSubscriber.unsubscribe()
    }
}