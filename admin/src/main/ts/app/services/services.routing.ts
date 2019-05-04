import { Routes } from '@angular/router';

import { ServicesComponent } from './services.component';

import { ApplicationsListComponent } from './applications/list/applications-list.component';
import { ApplicationDetailsComponent } from './applications/details/application-details.component';

import { ConnectorsListComponent } from './connectors/list/connectors-list.component';
import { ConnectorDetailsComponent } from './connectors/details/connector-details.component';
import { ConnectorCreate } from './connectors/create/connector-create.component';

import { ApplicationsResolver } from './applications/applications.resolver';
import { ConnectorsResolver } from './connectors/connectors.resolver';
import { ApplicationRolesResolver } from './applications/details/roles.resolver';
import { ConnectorRolesResolver } from './connectors/details/roles.resolver';

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
                        component: ApplicationDetailsComponent,
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
                    { path: 'create', component: ConnectorCreate },
                    {
                        path: ':connectorId',
                        component: ConnectorDetailsComponent,
                        resolve: {
                            roles: ConnectorRolesResolver
                        }
                    }
                ]
            }
        ]
    }
];
