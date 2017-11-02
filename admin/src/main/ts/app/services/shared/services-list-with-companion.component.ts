import { Component, ChangeDetectorRef, Input, Output,
    ContentChild, EventEmitter, AfterViewInit } from "@angular/core";

import { ActivatedRoute, Router,  Data } from '@angular/router';
import { SpinnerService, routing } from '../../core/services';
import { Subscription } from 'rxjs/Subscription';

import { ServicesStore } from '../services.store';

@Component({
    selector: 'services-list-with-companion',
    template: `
        <side-layout (closeCompanion)="closePanel()" [showCompanion]="showCompanion">
            <div side-card>
                <list-component
                [model]="collectionRef[serviceName].collection"
                sort="name"
                [inputFilter]="filterByInput"
                [searchPlaceholder]="collectionRef[serviceName].searchPlaceholder"
                [noResultsLabel]="collectionRef[serviceName].noResultsLabel"
                [isSelected]="isSelected"
                (inputChange)="itemInputFilter = $event"
                >
                    <ng-template let-item>
                        <div routerLink="{{item.id}}">
                            {{ item.name }}
                        </div>
                    </ng-template>
                </list-component>
            </div>
            <div side-companion>
                <router-outlet></router-outlet>
            </div>
        </side-layout>
    `,
    styles: [`

    `]
})
export class ServicesListWithCompanionComponent implements AfterViewInit {
    
    /* Store pipe */
    self = this;
    _storedElements = [];

    private filteredApps = [
        "Auth",
        "Cas",
        "Communication",
        "Directory",
        "Eliot",
        "FakeSSO",
        "Portal",
        "RSS",
        "Timeline",
        "Xiti"
    ];

    constructor(
        public cdRef: ChangeDetectorRef,
        private router: Router,
        private route: ActivatedRoute,
        public servicesStore: ServicesStore){}

    ngAfterViewInit() {
        this.cdRef.markForCheck();
        this.cdRef.detectChanges();
    }

    // TODO extract from router 
    @Input() serviceName: 'applications' | 'connectors' | 'widgets';

    collectionRef = {
        applications : {
            collection: this.servicesStore.structure.applications.data, 
            model:this.servicesStore.application, 
            routeData:'apps',
            searchPlaceholder:'search.application',
            noResultsLabel:'list.results.no.applications'
        },
        connectors : {
            collection: this.servicesStore.structure.connectors.data, 
            model:this.servicesStore.connector, 
            routeData:'connectors',
            searchPlaceholder:'search.connector',
            noResultsLabel:'list.results.no.connectors'
        }
    }

    @Output("onSelect") onSelect: EventEmitter<{}> = new EventEmitter();
    @Output("listChange") listChange: EventEmitter<any> = new EventEmitter();

    closePanel() {
        this.router.navigate(['..'], { relativeTo: this.route });
    }

    
    private routeSubscriber:Subscription;

    ngOnInit(): void {
        if (!this.serviceName) {
            throw new Error('Input property serviceName  is undefined. It must be set with one of "applications" | "connectors" | "widgets"')
        }
        this.routeSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data[this.collectionRef[this.serviceName].routeData]) {
                this.collectionRef[this.serviceName].collection = data[this.collectionRef[this.serviceName].routeData]
                    .filter(app => this.filteredApps.indexOf(app.name) < 0
                );
                this.cdRef.markForCheck();
            }
        })
    }

    ngOnDestroy(): void {
        this.routeSubscriber.unsubscribe();
    }


    itemInputFilter:string;
    filterByInput = (item: any) => {
        if(!this.itemInputFilter) return true;
        return item.name.toLowerCase()
            .indexOf(this.itemInputFilter.toLowerCase()) >= 0;
    }

    isSelected = (item) => {
        return this.collectionRef[this.serviceName].model == item;
    }

    showCompanion = (): boolean => {
        let basePath = `/admin/${this.servicesStore.structure.id}/services/${this.serviceName}`;
        if (this.collectionRef[this.serviceName].model)
            return this.router.isActive(`${basePath}\\${this.collectionRef[this.serviceName].model.id}`  , true);
        else 
            return false;
    }
}