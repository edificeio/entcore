import {Component, Input, OnDestroy, OnInit} from '@angular/core';

import {ActivatedRoute, Data, Router} from '@angular/router';
import {routing} from '../../../core/services/routing.service';
import {Subscription} from 'rxjs/';

import {ServicesStore} from '../../services.store';
import {InputFileService} from '../../../shared/ux/services';
import {BundlesService} from 'sijil';
import { ApplicationModel } from 'src/app/core/store/models/application.model';
import { ConnectorModel } from 'src/app/core/store/models/connector.model';
import { SessionModel } from 'src/app/core/store/models/session.model';

interface ServiceInfo {
    collection: any[];
    model: any;
    routeData: string;
    searchPlaceholder: string;
    noResultsLabel: string;
}

@Component({
    selector: 'ode-services-list',
    templateUrl: './services-list.component.html'
})
export class ServicesListComponent implements OnInit, OnDestroy{

    constructor(
        public router: Router,
        public route: ActivatedRoute,
        private servicesStore: ServicesStore,
        public inputFileService: InputFileService,
        private bundlesService: BundlesService) {
    }
    // TODO extract from router
    @Input()
    serviceName: 'applications' | 'connectors';
    @Input()
    selectedItem: ApplicationModel | ConnectorModel;

    public sortOnDisplayName = true;

    private routeSubscriber: Subscription;
    public collectionRef: { [serviceName: string]: ServiceInfo };

    itemInputFilter: string;

    ngOnInit(): void {
        this.sortOnDisplayName = this.serviceName !== 'applications';

        SessionModel.getSession().then(session => {
            if (!this.serviceName) {
                throw new Error('Input property serviceName is undefined. It must be set with one of "applications" | "connectors"');
            }
            this.routeSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
                if (data[this.collectionRef[this.serviceName].routeData]) {
                    this.collectionRef[this.serviceName].collection = data[this.collectionRef[this.serviceName].routeData];

                    if (this.serviceName === 'applications') {
                        this.collectionRef[this.serviceName].collection = filterApplicationsByLevelsOfEducation(
                            this.collectionRef[this.serviceName].collection,
                            this.servicesStore.structure.levelsOfEducation
                        );

                        this.collectionRef[this.serviceName].collection = filterApplicationsByType(
                            this.collectionRef[this.serviceName].collection,
                            session.functions.SUPER_ADMIN != null
                        );

                        if (!session.functions.SUPER_ADMIN) {
                            this.collectionRef[this.serviceName].collection = filterApplicationsByRoles(
                                this.collectionRef[this.serviceName].collection,
                                this.servicesStore.structure.distributions
                            );
                        }
                    }

                    this.collectionRef[this.serviceName].collection = this.collectionRef[this.serviceName].collection
                        .sort((a, b) => this.bundlesService.translate(a.displayName)
                            .localeCompare(this.bundlesService.translate(b.displayName)));
                }
            });
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
    filterByInput = (item: any): boolean => {
        return !!this.itemInputFilter ?
            this.bundlesService.translate(item.displayName).toLowerCase()
                .indexOf(this.itemInputFilter.toLowerCase()) >= 0 : true;
    }

    isSelected = (item): boolean => {
        return this.selectedItem && item && this.selectedItem.id === item.id;
    }

    showCompanion = (): boolean => {
        const basePath = `/admin/${this.servicesStore.structure.id}/services/${this.serviceName}`;
        if (this.collectionRef[this.serviceName].model) {
            return this.router.isActive(`${basePath}\\${this.collectionRef[this.serviceName].model.id}`, true);
        } else {
            return false;
        }
    }

    public isInherited(connector: ConnectorModel) {
        return connector.inherits && connector.structureId !== this.servicesStore.structure.id;
    }
}

export function filterApplicationsByLevelsOfEducation(apps: ApplicationModel[], levelsOfEducation: number[]): ApplicationModel[] {
    return apps.filter(app => levelsOfEducation.some(level => app.levelsOfEducation.indexOf(level) >= 0));
}

export function filterApplicationsByType(apps: ApplicationModel[], isAdmc: boolean): ApplicationModel[] {
    return apps.filter((app: ApplicationModel) => {
        if (isAdmc) {
            return app.appType === 'END_USER' || app.appType === 'SYSTEM';
        }
        return app.appType === 'END_USER';
    });
}

export function filterApplicationsByRoles(apps: ApplicationModel[], structureDistributions: string[]) {
    return apps.filter((app: ApplicationModel) => {
        if (app.roles.length === 0) {
            return false;
        }
        return app.roles.some(role => {
            return !role.distributions
                || role.distributions.length === 0
                || structureDistributions.some(structureDistribution => role.distributions.indexOf(structureDistribution) > -1);
        });
    });
}
