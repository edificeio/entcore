import {Routes} from '@angular/router';

import {ServicesComponent} from './services.component';
import {ApplicationsListComponent} from './applications/list/applications-list.component';
import {ConnectorsListComponent} from './connectors/list/connectors-list.component';
import {SmartConnectorComponent} from './connectors/connector/smart-connector/smart-connector.component';

import {ApplicationsResolver} from './applications/list/applications.resolver';
import {ConnectorsResolver} from './connectors/list/connectors.resolver';
import {ApplicationRolesResolver} from './applications/application/application-roles.resolver';
import {ConnectorRolesResolver} from './connectors/connector/connector-roles.resolver';
import {SmartApplicationComponent} from './applications/application/smart-application/smart-application.component';
import { WidgetsListComponent } from './widgets/list/widgets-list.component';
import { WidgetsResolver } from './widgets/list/widgets.resolver';
import { SmartWidgetComponent } from './widgets/smart-widget/smart-widget.component';
import { WidgetRolesResolver } from './widgets/smart-widget/widget-roles.resolver';
import { WidgetMyAppsParametersComponent } from './widgets/parameters/widget-myapps-parameters.component';
import { DefaultBookmarksResolver } from './widgets/parameters/default-bookmarks.resolver';

export let routes: Routes = [
    {
        path: '', component: ServicesComponent,
        children: [
            {
                path: '',
                redirectTo: 'applications',
                pathMatch: 'full'
            },
            {
                path: 'applications',
                component: ApplicationsListComponent,
                resolve: {apps: ApplicationsResolver},
                children: [
                    {
                        path: ':appId',
                        component: SmartApplicationComponent,
                        resolve: {
                            roles: ApplicationRolesResolver
                        }
                    }
                ]
            },
            {
                path: 'connectors',
                component: ConnectorsListComponent,
                resolve: {connectors: ConnectorsResolver},
                children: [
                    {   
                        path: 'create', 
                        component: SmartConnectorComponent 
                    },
                    {
                        path: ':connectorId',
                        component: SmartConnectorComponent,
                        resolve: {
                            roles: ConnectorRolesResolver
                        }
                    }
                ]
            },
            {
                path: 'widgets',
                component: WidgetsListComponent,
                resolve: {widgets: WidgetsResolver},
                children: [
                    {
                        path: ':widgetId',
                        component: SmartWidgetComponent,
                        resolve: {
                            roles: WidgetRolesResolver
                        },
                        children: [
                            {
                                path: 'myapps-params',
                                component: WidgetMyAppsParametersComponent,
                                resolve: {
                                    // my-apps parameters
                                    apps: ApplicationsResolver, 
                                    connectors: ConnectorsResolver,
                                    bookmarks: DefaultBookmarksResolver
                                }
                            }
                        ]
                    }
                ]
            }
        ]
    }
];
