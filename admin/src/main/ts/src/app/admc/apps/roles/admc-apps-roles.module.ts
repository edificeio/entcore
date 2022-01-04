import {NgModule} from '@angular/core';
import {CommonModule, NgSwitch} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { MatInputModule, MatSelectModule } from '@angular/material';
import {RouterModule} from '@angular/router';
import {NgxOdeSijilModule} from 'ngx-ode-sijil';
import {NgxOdeUiModule} from 'ngx-ode-ui';

import {routes} from './admc-apps-roles.routing';
import {AdmcAppsRolesStore} from './admc-apps-roles.store';
import {AdmcAppsRolesService} from './admc-apps-roles.service'

import {AdmcAppsRolesResolver} from './admc-apps-roles.resolver';
import {ApplicationRolesResolver} from './application-roles/application-roles.resolver';
import {RoleActionsResolver} from './application-roles/resolvers/roles.resolver';
import { DistributionsResolver } from './application-roles/resolvers/distributions.resolver';

import {AdmcAppsRolesComponent} from './admc-apps-roles.component';
import {ApplicationRolesComponent} from './application-roles/application-roles.component';
import {ApplicationsRoleCompositionComponent} from './applications-role-composition/applications-role-composition.component';
import { ActionsResolver } from './application-roles/resolvers/actions.resolver';

@NgModule({
    imports: [
        CommonModule,
        MatSelectModule,
        FormsModule,
        ReactiveFormsModule,
        MatInputModule,
        NgxOdeUiModule,
        NgxOdeSijilModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        AdmcAppsRolesComponent,
        ApplicationRolesComponent,
        ApplicationsRoleCompositionComponent,
    ],
    providers: [
        NgSwitch,
        AdmcAppsRolesStore,
        AdmcAppsRolesResolver,
        ApplicationRolesResolver,
        RoleActionsResolver,
        DistributionsResolver,
        AdmcAppsRolesService,
        ActionsResolver,
    ]
})
export class AdmcAppsRolesModule {
}
