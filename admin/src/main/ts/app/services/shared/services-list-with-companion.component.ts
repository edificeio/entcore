import { AfterViewInit, ChangeDetectorRef, Component, Input } from '@angular/core';

import { ActivatedRoute, Data, Router } from '@angular/router';
import { routing } from '../../core/services';
import { Subscription } from 'rxjs/Subscription';

import { ServicesStore } from '../services.store';
import { SessionModel } from '../../core/store';

interface ServiceInfo {
    collection: any[],
    model: any,
    routeData: string,
    searchPlaceholder: string,
    noResultsLabel: string
}

@Component({
    selector: 'services-list-with-companion',
    template: `
        <side-layout (closeCompanion)="closePanel()" [showCompanion]="showCompanion">
            <div side-card>
                <list
                        [model]="collectionRef[serviceName].collection"
                        sort="name"
                        [inputFilter]="filterByInput"
                        [searchPlaceholder]="collectionRef[serviceName].searchPlaceholder"
                        [noResultsLabel]="collectionRef[serviceName].noResultsLabel"
                        [isSelected]="isSelected"
                        (inputChange)="itemInputFilter = $event"
                        (onSelect)="selectedItem = $event; router.navigate([$event.id], {relativeTo: route})">
                    <ng-template let-item>
                        <div class="service-icon">
                            <img [src]="item.icon" *ngIf="isIconWorkspaceImg(item.icon)"/>
                            <i ngClass="item.icon" *ngIf="!isIconWorkspaceImg(item.icon)"></i>
                        </div>
                        <div class="service-name">
                            {{ item.name }}
                        </div>
                    </ng-template>
                </list>
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
        'Auth',
        'Application Mobile',
        'AppRegistry',
        'Cas',
        'Communication',
        'Directory',
        'Eliot',
        'FakeSSO',
        'Portal',
        'Rss',
        'Searchengine',
        'Signets',
        'Starter',
        'Supportpivot',
        'Timeline',
        'Xiti'
    ];

    constructor(
        private cdRef: ChangeDetectorRef,
        public router: Router,
        public route: ActivatedRoute,
        private servicesStore: ServicesStore) {
    }

    ngAfterViewInit() {
        this.cdRef.markForCheck();
        this.cdRef.detectChanges();
    }

    // TODO extract from router 
    @Input() serviceName: 'applications' | 'connectors' | 'widgets';

    collectionRef: { [serviceName: string]: ServiceInfo };

    @Input() selectedItem;

    closePanel(): void {
        this.router.navigate(['..'], {relativeTo: this.route});
    }

    private routeSubscriber: Subscription;

    ngOnInit(): void {
        // HAck : display app just if user is ADMC => TODO implement a directive or a component
        // Session is already fetched in nav.component and must be shared instead of being requested again
        SessionModel.getSession().then(session => {
            if (!session.functions['SUPER_ADMIN']) {
                this.filteredApps.push('Admin', 'Administration', 'ABSENCES', 'NOTES', 'SCOLARITE', 'TEXTES', 'AGENDA');
            }
            if (!this.serviceName) {
                throw new Error('Input property serviceName is undefined. It must be set with one of "applications" | "connectors" | "widgets"')
            }
            this.routeSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
                if (data[this.collectionRef[this.serviceName].routeData]) {
                    this.collectionRef[this.serviceName].collection = data[this.collectionRef[this.serviceName].routeData]
                        .filter(app => this.filteredApps.indexOf(app.name) < 0);
                    this.cdRef.markForCheck();
                }
            })
        });

        this.collectionRef = {
            applications: {
                collection: this.servicesStore.structure.applications.data,
                model: this.servicesStore.application,
                routeData: 'apps',
                searchPlaceholder: 'services.application.search',
                noResultsLabel: 'services.application.list.empty'
            },
            connectors: {
                collection: this.servicesStore.structure.connectors.data,
                model: this.servicesStore.connector,
                routeData: 'connectors',
                searchPlaceholder: 'services.connector.search',
                noResultsLabel: 'services.connector.list.empty'
            }
        };
    }

    ngOnDestroy(): void {
        this.routeSubscriber.unsubscribe();
    }


    itemInputFilter: string;
    filterByInput = (item: any): boolean => {
        return !!this.itemInputFilter ?
            item.name.toLowerCase().indexOf(this.itemInputFilter.toLowerCase()) >= 0 : true;
    };

    isSelected = (item): boolean => {
        return this.selectedItem && item && this.selectedItem.id === item.id;
    };

    showCompanion = (): boolean => {
        let basePath = `/admin/${this.servicesStore.structure.id}/services/${this.serviceName}`;
        if (this.collectionRef[this.serviceName].model) {
            return this.router.isActive(`${basePath}\\${this.collectionRef[this.serviceName].model.id}`, true);
        } else {
            return false;
        }
    };

    private isIconWorkspaceImg(src: String) {
        return src.startsWith('/workspace');
    }
}