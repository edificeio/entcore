import { Component, EventEmitter, Injector, Input, Output } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { OdeComponent } from 'ngx-ode-core';
import { SelectOption } from 'ngx-ode-ui';
import { ApplicationModel } from 'src/app/core/store/models/application.model';
import { WidgetModel } from 'src/app/core/store/models/widget.model';
import { Profile, Structure } from '../../_shared/services-types';
import { routing } from "src/app/core/services/routing.service";
import { Data, Params } from "@angular/router";
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { ServicesStore } from '../../services.store';
import { WidgetService } from 'src/app/core/services/widgets.service';
import { filterApplicationsByLevelsOfEducation, filterApplicationsByRoles, filterApplicationsByType } from '../../_shared/services-list/services-list.component';
import { IConnector } from 'src/app/core/store/models/connector.model';


@Component({
    selector: 'ode-widget-myapps-parameters',
    templateUrl: 'widget-myapps-parameters.component.html',
    styleUrls: [
        'widget-myapps-parameters.component.scss'
    ]
})

export class WidgetMyAppsParametersComponent extends OdeComponent {
    public structure:Structure;
    public widget: WidgetModel;

    public addApplicationForm: FormGroup = new FormGroup({
        selectedApp: new FormControl(),
        profiles: new FormControl()
    });
    
    public selectedApp:ApplicationModel = null;
    public applicationOptions: Array<SelectOption<ApplicationModel>>;
    public connectorOptions: Array<SelectOption<IConnector>>;

    public selectedProfiles: Array<Profile> = [];
    public profileOptions: Array<SelectOption<string>> = [];
    public profileTrackByFn = (p: Profile) => p;

    public showAppLightbox:boolean = false;
    public showConnectorLightbox:boolean = false;

    //TODO private bookmarksModel:any = {};

    constructor(
            injector: Injector,
            public servicesStore: ServicesStore,
            private widgetSvc:WidgetService
        ) {
        super(injector);
        this.reset();
    }

    ngOnInit() {
        // Retrieve selected profiles in the lightboxes
        this.addApplicationForm.get('profiles').valueChanges.subscribe((profiles: Array<Profile>) => {
            this.selectedProfiles = profiles || [];
        });

        // Listen to widget changes in the route
        this.subscriptions.add(this.route.params.subscribe((params: Params) => {
            if (params['widgetId']) {
                this.widget = this.servicesStore.structure.widgets.data.find(a => a.id === params['widgetId']);
                this.updateBookmarks();
            }
        }));

        // Listen to structure changes in the route
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.updateAppList();
                this.updateConnectorList();
            }
        }));

        this.updateAppList();
        this.updateConnectorList();
    }

        /** Get all apps from the route resolver, and filter them. */
        private updateAppList() {
        let apps:Array<ApplicationModel> = this.route.snapshot.data.apps;
        apps = filterApplicationsByLevelsOfEducation( apps, this.servicesStore.structure.levelsOfEducation );
        apps = filterApplicationsByType( apps, false /*ADMC cannot modify myapps preferences*/ );
        apps = filterApplicationsByRoles( apps, this.servicesStore.structure.distributions );

        this.applicationOptions = apps.sort((a, b) => {
            return (a && a.displayName && b && b.displayName) ? a.displayName.localeCompare(b.displayName) : 0;
        })
        .map( app => ({label:app.displayName, value:app}));
    }

    /** Get all connectors from the route resolver, and filter them. */
    private updateConnectorList() {
        let connectors:Array<IConnector> = this.route.snapshot.data.connectors;
        this.connectorOptions = connectors.sort((a, b) => {
            return (a && a.displayName && b && b.displayName) ? a.displayName.localeCompare(b.displayName) : 0;
        })
        .map( app => ({label:app.displayName, value:app}));
    }

    private updateBookmarks() {
        if( this.structure ) this.widgetSvc.getMyAppsParameters(this.structure).then( bookmarks => {
            //TODO this.bookmarksModel = ...
        })
    }

    public onApplicationChange(e) {
        this.selectedApp = e;
    }

    public addApplication() {
        // TODO Update this.bookmarksModel then call this.widgetSvc.setMyAppsParameters()
        //alert( JSON.stringify(this.selectedProfiles) ) ;
    }

    /** [Re]init the app/connectors lightboxes. */
    public reset() {
        this.selectedApp = null;
        this.profileOptions = ['Guest', 'Personnel', 'Relative', 'Student', 'Teacher', 'AdminLocal'].map( p => ({
            value:p, 
            label:p
        }) );
        this.addApplicationForm.get('profiles').reset();
        this.showAppLightbox = this.showConnectorLightbox = false;
    }

}