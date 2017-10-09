import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,
    ChangeDetectorRef } from "@angular/core";

import { ActivatedRoute, Router,  Data } from '@angular/router';

import { Subscription } from 'rxjs/Subscription';
import { SpinnerService, routing } from '../../../core/services';
import { globalStore, ConnectorModel } from '../../../core/store';

import { ServicesStore } from '../../services.store';

@Component({
    selector: 'apps-list',
    template: `
        <services-list-with-companion
            [showCompanion]="showDetails()"
            [model]="servicesStore.structure.connectors.data"
            sort="name"
            [inputFilter]="filterByInput"
            searchPlaceholder="search.connector"
            noResultsLabel="list.results.no.connector"
            [isSelected]="isSelected"
            (inputChange)="connectorInputFilter = $event"
            (onSelect)="routeToConnectorn($event)"
        >
        </services-list-with-companion>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConnectorsListComponent implements OnInit, OnDestroy {
    
    private connectorsSubscriber: Subscription;
    connectorInputFilter: string;
    iconsRoot: string = "/assets/themes/panda/img/icons/";

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private ls: SpinnerService,
        public servicesStore: ServicesStore
    ) {}

    ngOnInit(): void {
        this.connectorsSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['connectors']) {
                this.servicesStore.structure.connectors.data = data['connectors'];
                this.cdRef.markForCheck();
            }
        })
    }

    ngOnDestroy(): void {
        this.connectorsSubscriber.unsubscribe();
    }

    isSelected = (connector) => {
        return this.servicesStore.connector === connector;
    }

    routeToConnectorn(connector: ConnectorModel) {
        this.servicesStore.connector = connector;
        this.router.navigate([connector.id], { relativeTo: this.route });
    }

    filterByInput = (connector: any) => {
        if(!this.connectorInputFilter) return true;
        return connector.name.toLowerCase()
            .indexOf(this.connectorInputFilter.toLowerCase()) >= 0;
    }

    showDetails = (): boolean => {
        const connectorsRoute = '/admin/' + 
        (this.servicesStore.structure ? this.servicesStore.structure.id : '') + 
        '/services/connectors';

        let res: boolean = false;
        if (this.servicesStore.connector)
            res = this.router.isActive(connectorsRoute + '/' + this.servicesStore.connector.id, true);

        return res;
    }
}