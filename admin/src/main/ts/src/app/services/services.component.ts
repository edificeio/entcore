import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {ServicesStore} from './services.store';
import {routing} from '../core/services';
import {ServicesService} from './services.service';

@Component({
    selector: 'ode-services-root',
    templateUrl: './services.component.html',
    providers: [ServicesService]
})
export class ServicesComponent implements OnInit, OnDestroy {

    private structureSubscriber: Subscription;

    tabs: Array<{ label: string, view: string, disabled: boolean }> = [
        {label: 'applications', view: 'applications', disabled: false},
        {label: 'connectors', view: 'connectors', disabled: false}
    ];

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private servicesStore: ServicesStore) {
    }

    ngOnInit(): void {
        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.servicesStore.structure = data.structure;
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
