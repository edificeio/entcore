import { Routes } from '@angular/router';

import { ServicesComponent } from './services.component';
import { ApplicationsListComponent } from './applications/list/applications-list.component';
import { ApplicationAssignmentComponent } from './applications/application/assignment/application-assignment.component';
import { ConnectorsListComponent } from './connectors/list/connectors-list.component';
import { SmartConnectorComponent } from './connectors/connector/smart-connector.component';

import { ApplicationsResolver } from './applications/list/applications.resolver';
import { ConnectorsResolver } from './connectors/list/connectors.resolver';
import { ApplicationRolesResolver } from './applications/application/application-roles.resolver';
import { ConnectorRolesResolver } from './connectors/connector/connector-roles.resolver';
import { SmartApplicationComponent } from './applications/application/smart-application.component';

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
                    { path: 'create', component: SmartConnectorComponent },
                    {
                        path: ':connectorId',
                        component: SmartConnectorComponent,
                        resolve: {
                            roles: ConnectorRolesResolver
                        }
                    }
                ]
            }
        ]
    }
];
