import { Component, Input } from '@angular/core';

import { ActivatedRoute, Data, Router } from '@angular/router';
import { routing } from '../../core/services';
import { Subscription } from 'rxjs/Subscription';

import { ServicesStore } from '../services.store';
import { ApplicationModel, ConnectorModel, SessionModel } from '../../core/store';
import { InputFileService } from '../../shared/ux/services';
import { BundlesService } from 'sijil';

interface ServiceInfo {
    collection: any[],
    model: any,
    routeData: string,
    searchPlaceholder: string,
    noResultsLabel: string
}

@Component({
    selector: 'services-list',
    template: `
        <side-layout (closeCompanion)="closePanel()" [showCompanion]="showCompanion">
            <div side-card>
                <list
                        [model]="collectionRef[serviceName].collection"
                        [sort]="sortOnDisplayName? 'displayName' : null"
                        [inputFilter]="filterByInput"
                        [searchPlaceholder]="collectionRef[serviceName].searchPlaceholder"
                        [noResultsLabel]="collectionRef[serviceName].noResultsLabel"
                        [isSelected]="isSelected"
                        (inputChange)="itemInputFilter = $event"
                        (onSelect)="selectedItem = $event; router.navigate([$event.id], {relativeTo: route})">
                    <ng-template let-item>
                        <div class="service-badges" *ngIf="serviceName === 'connectors'">
                            <i *ngIf="isInherited(item)"
                               class="fa fa-link service-badges__inherits"
                               [title]="'services.connector.inherited' | translate"></i>
                            <i *ngIf="item.locked"
                               class="fa fa-lock service-badges__locked"
                               [title]="'services.connector.locked' | translate"></i>
                        </div>
                        <div class="service-icon">
                            <img [src]="item.icon" *ngIf="inputFileService.isSrcExternalUrl(item.icon)"/>
                            <img src="{{ item.icon }}" *ngIf="inputFileService.isSrcWorkspace(item.icon)"/>
                            <i [ngClass]="item.icon"
                               *ngIf="!inputFileService.isSrcExternalUrl(item.icon)
                                    && !inputFileService.isSrcWorkspace(item.icon)"></i>
                        </div>
                        <div class="service-name">
                            <span>{{ item.displayName | translate }}</span>
                        </div>
                    </ng-template>
                </list>
            </div>
            <div side-companion>
                <router-outlet></router-outlet>
            </div>
        </side-layout>
    `
})
export class ServicesListComponent {
    // TODO extract from router 
    @Input()
    serviceName: 'applications' | 'connectors';
    @Input()
    selectedItem: ApplicationModel | ConnectorModel;

    public sortOnDisplayName = true;

    private routeSubscriber: Subscription;
    public collectionRef: { [serviceName: string]: ServiceInfo };

    constructor(
        public router: Router,
        public route: ActivatedRoute,
        private servicesStore: ServicesStore,
        public inputFileService: InputFileService,
        private bundlesService: BundlesService) {
    }

    ngOnInit(): void {
        this.sortOnDisplayName = this.serviceName !== 'applications';

        SessionModel.getSession().then(session => {
            if (!this.serviceName) {
                throw new Error('Input property serviceName is undefined. It must be set with one of "applications" | "connectors"')
            }
            this.routeSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
                if (data[this.collectionRef[this.serviceName].routeData]) {
                    this.collectionRef[this.serviceName].collection = data[this.collectionRef[this.serviceName].routeData];

                    if (this.serviceName === 'applications') {
                        this.collectionRef[this.serviceName].collection = filterApplicationsByLevelsOfEducation(
                            this.collectionRef[this.serviceName].collection,
                            this.servicesStore.structure.levelsOfEducation
                        );

                        this.collectionRef[this.serviceName].collection = filterApplicationsByType(
                            this.collectionRef[this.serviceName].collection,
                            session.functions['SUPER_ADMIN'] != null
                        );
                    }

                    this.collectionRef[this.serviceName].collection = this.collectionRef[this.serviceName].collection
                        .sort((a, b) => this.bundlesService.translate(a.displayName)
                            .localeCompare(this.bundlesService.translate(b.displayName)));
                }
            })
        });

        this.collectionRef = {
            applications: {
                collection: filterApplicationsByLevelsOfEducation(
                    this.servicesStore.structure.applications.data,
                    this.servicesStore.structure.levelsOfEducation
                ),
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

    closePanel(): void {
        this.router.navigate(['..'], {relativeTo: this.route});
    }

    itemInputFilter: string;
    filterByInput = (item: any): boolean => {
        return !!this.itemInputFilter ?
            this.bundlesService.translate(item.displayName).toLowerCase()
                .indexOf(this.itemInputFilter.toLowerCase()) >= 0 : true;
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

    public isInherited(connector: ConnectorModel) {
        return connector.inherits && connector.structureId != this.servicesStore.structure.id;
    }
}

export function filterApplicationsByLevelsOfEducation(apps: ApplicationModel[], levelsOfEducation: number[]): ApplicationModel[] {
    return apps.filter(app => levelsOfEducation.some(level => app.levelsOfEducation.indexOf(level) >= 0));
}

export function filterApplicationsByType(apps: ApplicationModel[], isAdmc: boolean): ApplicationModel[] {
    return apps.filter((app: ApplicationModel) => {
        if (isAdmc) {
            return app.appType == 'END_USER' || app.appType == 'SYSTEM';
        }
        return app.appType == 'END_USER';
    });
}
