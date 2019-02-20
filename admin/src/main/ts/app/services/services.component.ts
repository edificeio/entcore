import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Data, NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { ServicesStore } from './services.store';
import { routing } from '../core/services';

@Component({
    selector: 'services-root',
    template: `
        <h1><i class="fa fa-th"></i> {{ 'services' | translate }}</h1>
        <div class="tabs">
            <button class="tab" *ngFor="let tab of tabs"
                    [disabled]="tab.disabled"
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

    private structureSubscriber: Subscription;
    private routerSubscriber: Subscription;

    tabs: Array<{ label: string, view: string, disabled: boolean }> = [
        {label: 'Applications', view: 'applications', disabled: false},
        {label: 'Connecteurs', view: 'connectors', disabled: false},
        {label: 'Widgets', view: 'widgets', disabled: true}
    ];

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private servicesStore: ServicesStore) {
    }

    ngOnInit(): void {
        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data['structure']) {
                this.servicesStore.structure = data['structure'];
                this.cdRef.markForCheck();
            }
        });

        this.routerSubscriber = this.router.events.subscribe(event => {
            if (event instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        });
    }

    ngOnDestroy(): void {
        this.structureSubscriber.unsubscribe();
        this.routerSubscriber.unsubscribe();
    }
}