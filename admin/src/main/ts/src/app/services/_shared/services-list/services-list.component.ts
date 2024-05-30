import { Component, Injector, Input, OnDestroy, OnInit } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { BundlesService } from 'ngx-ode-sijil';
import { InputFileService } from 'ngx-ode-ui';
import { ApplicationModel } from 'src/app/core/store/models/application.model';
import { ConnectorModel } from 'src/app/core/store/models/connector.model';
import { SessionModel } from 'src/app/core/store/models/session.model';
import { WidgetModel } from 'src/app/core/store/models/widget.model';
import { routing } from '../../../core/services/routing.service';
import { ServicesStore } from '../../services.store';

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
export class ServicesListComponent extends OdeComponent implements OnInit, OnDestroy {

    constructor(
        injector: Injector,
        private servicesStore: ServicesStore,
        public inputFileService: InputFileService,
        private bundlesService: BundlesService) {
            super(injector);
    }
    // TODO extract from router
    @Input()
    serviceName: 'applications' | 'connectors' | 'widgets';
    @Input()
    selectedItem: ApplicationModel | ConnectorModel | WidgetModel;

    public collectionRef: { [serviceName: string]: ServiceInfo };

    private iconWidget: {[T: string]: string} = {
        "agenda-widget":        "agenda",
        "birthday":             "birthday",
        "bookmark-widget":      "signets",
        "calendar-widget":      "calendar",
        "carnet-de-bord":       "carnet-de-bord",
        "cursus-widget":        "aide-devoirs",
        "last-infos-widget":    "actualites",
        "maxicours-widget":     "maxicours",
        "dicodelazone-widget":  "dicodelazone",
        "mood":                 "mood",
        "my-apps":              "apps",
        "notes":                "notes",
        "qwant":                "qwant",
        "qwant-junior":         "qwant-junior",
        "rss-widget":           "rss",
        "record-me":            "dictaphone",
        "school-widget":        "schoolbook",
        "edumalin-widget":      "edumalin",
        "mediacentre-widget":   "mediacentre"
    }

    itemInputFilter: string;

    ngOnInit(): void {
        super.ngOnInit();

        SessionModel.getSession().then(session => {
            if (!this.serviceName) {
                throw new Error('Input property serviceName is undefined. It must be set with one of "applications" | "connectors" | "widgets"');
            }
            this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
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
                    } else if( this.serviceName === "widgets" ) {
                        // A SUPER_ADMIN can see all widgets ;
                        if( !session.functions.SUPER_ADMIN ) {
                            // A non-SUPER_ADMIN can only see widgets with the same level of education than the current structure.
                            this.collectionRef[this.serviceName].collection = filterWidgetsByLevelsOfEducation(
                                this.collectionRef[this.serviceName].collection,
                                this.servicesStore.structure.levelsOfEducation
                            )
                        }

                        // We add icon property because doesn't exist on WidgetModel 
                        this.collectionRef[this.serviceName].collection = addWidgetsIcons(this.collectionRef[this.serviceName].collection, this.iconWidget);

                    }
                    
                }
            }));
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
            },
            widgets: {
                collection: this.servicesStore.structure.widgets.data,
                model: this.servicesStore.widget,
                routeData: 'widgets',
                searchPlaceholder: 'services.widget.search',
                noResultsLabel: 'services.widget.list.empty'
            }
        };
        
    }

    closePanel(): void {
        this.router.navigate(['..'], {relativeTo: this.route});
    }
    filterByInput = (item: any): boolean => {
        return !!this.itemInputFilter ?
            this.bundlesService.translate(item.displayName).toLowerCase()
                .indexOf(this.itemInputFilter.toLowerCase()) >= 0 : true;
    }
    sortByDisplayName = () => {
        this.collectionRef[this.serviceName].collection
        .sort((a, b) => {
            if (a && a.displayName && b && b.displayName) {
                return this.bundlesService.translate(a.displayName).localeCompare(this.bundlesService.translate(b.displayName));
            } else {
                return -1;
            }
        });
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

export function filterWidgetsByLevelsOfEducation(widgets: WidgetModel[], levelsOfEducation: number[]): WidgetModel[] {
    if( !levelsOfEducation || levelsOfEducation.length===0 ) {
        return widgets;
    }
    return widgets.filter(widget => levelsOfEducation.some(level => widget.levelsOfEducation.indexOf(level) >= 0));
}

export function addWidgetsIcons(widgets: WidgetModel[], iconWidget:{[T: string]: string}) {
    return widgets.map(widget => ({ ...widget, icon: iconWidget[widget.displayName] }));
}