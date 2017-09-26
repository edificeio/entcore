import { Routes } from '@angular/router'

import { ServicesComponent } from './services.component'
import { ApplicationsListComponent } from './applications/list/applications-list.component'
import { ApplicationDetailsComponent } from './applications/details/application-details.component'

import { ApplicationsResolver } from './applications/applications.resolver'
import { RolesResolver } from './applications/details/roles.resolver'

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
                resolve: { apps: ApplicationsResolver },
                children: [
                    {
                        path: ':appId', 
                        component: ApplicationDetailsComponent,
                        resolve: { 
                            roles: RolesResolver
                        }
                    }
                ]
            },
            /*{
                path: 'applications/:appId', 
                component: ApplicationsMainListComponent,
                children: [
                    {
                        path: '', 
                        component: ApplicationDetailsComponent,
                        resolve: { 
                            details: ApplicationDetailsResolver, 
                            roles: RolesResolver
                        }
                    }
                ]
            },
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