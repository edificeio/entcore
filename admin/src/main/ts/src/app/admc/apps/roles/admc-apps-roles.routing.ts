import {Routes} from '@angular/router';

import {AdmcAppsRolesComponent as AdmcAppsRolesComponent} from './admc-apps-roles.component';
import { ApplicationRolesComponent } from './application-roles/application-roles.component';

import {AdmcAppsRolesResolver} from './admc-apps-roles.resolver';
import {ApplicationRolesResolver} from './application-roles/application-roles.resolver';
import { RoleActionsResolver } from './application-roles/resolvers/roles.resolver';
import { DistributionsResolver } from './application-roles/resolvers/distributions.resolver';
import { ActionsResolver } from './application-roles/resolvers/actions.resolver';

export let routes: Routes = [
    {
        path: '', component: AdmcAppsRolesComponent,
        resolve: { apps: AdmcAppsRolesResolver },
        children: [{
            path: ':appId',
            component: ApplicationRolesComponent,
            resolve: { 
                app: ApplicationRolesResolver, 
                roles: RoleActionsResolver, 
                actions: ActionsResolver,
                distributions: DistributionsResolver
            }
        }]
    }
];
