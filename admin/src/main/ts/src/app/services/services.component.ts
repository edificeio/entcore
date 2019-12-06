import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from '../core/services/routing.service';
import { ServicesService } from './services.service';
import { ServicesStore } from './services.store';

@Component({
    selector: 'ode-services-root',
    templateUrl: './services.component.html',
    providers: [ServicesService]
})
export class ServicesComponent extends OdeComponent implements OnInit, OnDestroy {


    tabs: Array<{ label: string, view: string, disabled: boolean }> = [
        {label: 'applications', view: 'applications', disabled: false},
        {label: 'connectors', view: 'connectors', disabled: false}
    ];

    constructor(
        injector: Injector,
        private servicesStore: ServicesStore) {
            super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.servicesStore.structure = data.structure;
            }
        }));
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
