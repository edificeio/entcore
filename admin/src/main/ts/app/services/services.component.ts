import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Data, Router } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { ServicesStore } from './services.store';
import { routing } from '../core/services';
import { ServicesService } from './services.service';

@Component({
    selector: 'services-root',
    template: `
        <div class="flex-header">
            <h1><i class="fa fa-th"></i> {{ 'services' | translate }}</h1>
                
            <button *ngIf="showCreateConnectorButton()" [routerLink]="['connectors', 'create']">
                <s5l>services.connector.create.button</s5l>
                <i class="fa fa-plug is-size-5"></i>
            </button>
        </div>

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
    providers: [ServicesService]
})
export class ServicesComponent implements OnInit, OnDestroy {

    private structureSubscriber: Subscription;

    tabs: Array<{ label: string, view: string, disabled: boolean }> = [
        {label: 'Applications', view: 'applications', disabled: false},
        {label: 'Connecteurs', view: 'connectors', disabled: false}
    ];

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private servicesStore: ServicesStore) {
    }

    ngOnInit(): void {
        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data['structure']) {
                this.servicesStore.structure = data['structure'];
            }
        });
    }

    ngOnDestroy(): void {
        this.structureSubscriber.unsubscribe();
    }

    public showCreateConnectorButton(): boolean {
        if (this.router.isActive(`/admin/${this.servicesStore.structure.id}/services/connectors/create`, true)) {
            return false;
        }
        if (this.router.isActive(`/admin/${this.servicesStore.structure.id}/services/connectors`, false)) {
            return true;
        }
        return false;
    }
}