import { NgModule } from '@angular/core';
import { CommonModule, NgSwitch } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SijilModule } from 'sijil';

import { UxModule } from '../shared/ux/ux.module';
import { routes } from './services.routing';

import { ServicesStore } from './services.store';

import { ApplicationsResolver } from './applications/list/applications.resolver';
import { ConnectorsResolver } from './connectors/list/connectors.resolver';
import { ApplicationRolesResolver } from './applications/application/application-roles.resolver';
import { ConnectorRolesResolver } from './connectors/connector/connector-roles.resolver';

import { ServicesComponent } from './services.component';
import { ServicesListComponent, ServicesRoleAttributionComponent, ServicesRoleComponent } from './shared/';

import { ApplicationsListComponent } from './applications/list/applications-list.component';
import { SmartApplicationComponent } from './applications/application/smart-application.component'
import { ApplicationAssignmentComponent } from './applications/application/assignment/application-assignment.component';
import { SmartMassRoleAssignment } from './applications/application/mass-assignment/smart-mass-role-assignment.component';
import { MassRoleAssignment } from './applications/application/mass-assignment/mass-role-assignment.component';

import { ConnectorsListComponent } from './connectors/list/connectors-list.component';
import { SmartConnectorComponent } from './connectors/connector/smart-connector.component'
import { ConnectorPropertiesComponent } from './connectors/connector/properties/connector-properties.component';
import { ConnectorAssignmentComponent } from './connectors/connector/assignment/connector-assignment.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        UxModule,
        SijilModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        ServicesComponent,
        SmartApplicationComponent,
        ApplicationsListComponent,
        ApplicationAssignmentComponent,
        MassRoleAssignment,
        SmartMassRoleAssignment,
        ConnectorsListComponent,
        ConnectorAssignmentComponent,
        ServicesListComponent,
        ServicesRoleComponent,
        ServicesRoleAttributionComponent,
        SmartConnectorComponent,
        ConnectorPropertiesComponent
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
