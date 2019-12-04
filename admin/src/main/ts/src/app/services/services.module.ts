import {NgModule} from '@angular/core';
import {CommonModule, NgSwitch} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {NgxOdeSijilModule} from 'ngx-ode-sijil';

import {NgxOdeUiModule} from 'ngx-ode-ui';
import {routes} from './services.routing';

import {ServicesStore} from './services.store';

import {ApplicationsResolver} from './applications/list/applications.resolver';
import {ConnectorsResolver} from './connectors/list/connectors.resolver';
import {ApplicationRolesResolver} from './applications/application/application-roles.resolver';
import {ConnectorRolesResolver} from './connectors/connector/connector-roles.resolver';

import {ServicesComponent} from './services.component';

import {ApplicationsListComponent} from './applications/list/applications-list.component';
import {SmartApplicationComponent} from './applications/application/smart-application/smart-application.component';
import {ApplicationAssignmentComponent} from './applications/application/assignment/application-assignment.component';
import {MassRoleAssignment} from './applications/application/mass-assignment/mass-role-assignment/mass-role-assignment.component';

import {ConnectorsListComponent} from './connectors/list/connectors-list.component';
import {SmartConnectorComponent} from './connectors/connector/smart-connector/smart-connector.component';
import {ConnectorPropertiesComponent} from './connectors/connector/properties/connector-properties.component';
import {ConnectorAssignmentComponent} from './connectors/connector/assignment/connector-assignment.component';
import {ConnectorMassAssignmentComponent} from './connectors/connector/mass-assignment/connector-mass-assignment.component';
import {ConnectorExportComponent} from './connectors/connector/export/connector-export.component';
import {SmartMassRoleAssignmentComponent} from './applications/application/mass-assignment/smart-mass-role-assignment/smart-mass-role-assignment.component';
import { ServicesListComponent } from './shared/services-list/services-list.component';
import { ServicesRoleComponent } from './shared/services-role/services-role.component';
import { ServicesRoleAttributionComponent } from './shared/services-role-attribution/services-role-attribution.component';
import { CoreModule } from '../core/core.module';

@NgModule({
    imports: [
        CoreModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        NgxOdeUiModule,
        NgxOdeSijilModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        ServicesComponent,
        SmartApplicationComponent,
        ApplicationsListComponent,
        ApplicationAssignmentComponent,
        MassRoleAssignment,
        SmartMassRoleAssignmentComponent,
        ConnectorsListComponent,
        ConnectorAssignmentComponent,
        ServicesListComponent,
        ServicesRoleComponent,
        ServicesRoleAttributionComponent,
        SmartConnectorComponent,
        ConnectorPropertiesComponent,
        ConnectorMassAssignmentComponent,
        ConnectorExportComponent
    ],
    providers: [
        NgSwitch,
        ServicesStore,
        ApplicationsResolver,
        ConnectorsResolver,
        ApplicationRolesResolver,
        ConnectorRolesResolver
    ]
})
export class ServicesModule {
}
