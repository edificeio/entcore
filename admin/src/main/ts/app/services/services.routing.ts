import { Routes } from '@angular/router'

import { ServicesComponent } from './services.component'
import { ApplicationsDetailsListComponent } from './applications/details/applications-details-list.component'
import { ApplicationsMainListComponent } from './applications/list/applications-main-list.component'
import { ApplicationDetailsComponent } from './applications/details/application-details.component'

import { ApplicationsResolver } from './applications/applications.resolver'
import { ApplicationDetailsResolver } from './applications/details/application-details.resolver'
import { ApplicationActionsResolver } from './applications/details/application-actions.resolver'

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
                component: ApplicationsMainListComponent, 
                resolve: { apps: ApplicationsResolver }
            },
            {
                path: 'applications/:appId', 
                component: ApplicationsDetailsListComponent,
                children: [
                    {
                        path: '', 
                        component: ApplicationDetailsComponent,
                        resolve: { 
                            appDetails: ApplicationDetailsResolver, 
                            appActions: ApplicationActionsResolver 
                        }
                    }
                ]
            }/*,
            { 
                path: 'widgets', component: WidgetsListComponent,
                children: [{

                }]
            },
            { 
                path: 'connectors', component: ConnectorsListComponent,
                children: [{

                }]
            }*/]
    }
]