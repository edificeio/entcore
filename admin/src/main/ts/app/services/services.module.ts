import { NgModule } from '@angular/core';
import { CommonModule, NgSwitch } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SijilModule } from 'sijil';

import { UxModule } from '../shared/ux/ux.module';
import { routes } from './services.routing';

import { ServicesStore } from './services.store';
import { ApplicationsResolver } from './applications/applications.resolver';
import { ConnectorsResolver } from './connectors/connectors.resolver';
import { ApplicationRolesResolver } from './applications/details/roles.resolver';
import { ConnectorRolesResolver } from './connectors/details/roles.resolver';

import { ServicesComponent } from './services.component';
import { ApplicationsListComponent } from './applications/list/applications-list.component';
import { ConnectorsListComponent } from './connectors/list/connectors-list.component';
import { ApplicationDetailsComponent } from './applications/details/application-details.component';
import { MassRoleAssignment } from './applications/details/mass-role-assignment.component';
import { SmartMassRoleAssignment } from './applications/details/smart-mass-role-assignment.component';
import { ConnectorDetailsComponent } from './connectors/details/connector-details.component';
import { ServicesListWithCompanionComponent, ServicesRoleAttributionComponent, ServicesRoleComponent } from './shared/';

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
        ApplicationsListComponent,
        ApplicationDetailsComponent,
        MassRoleAssignment,
        SmartMassRoleAssignment,
        ConnectorsListComponent,
        ConnectorDetailsComponent,
        ServicesListWithCompanionComponent,
        ServicesRoleComponent,
        ServicesRoleAttributionComponent
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
