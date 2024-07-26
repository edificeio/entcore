import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {NgModule} from '@angular/core';
import {NgxOdeSijilModule} from 'ngx-ode-sijil';
import {NgxOdeUiModule} from 'ngx-ode-ui';
import {routes} from './users.routing';
import {UserDetailsResolver} from './details/user-details.resolver';
import {UsersResolver, RemovedUsersResolver} from './users.resolver';
import {UsersComponent} from './users.component';
import {UserCreateComponent} from './create/user-create.component';
import {UserDetailsComponent} from './details/user-details.component';
import {SimpleUserDetailsComponent} from './simple-details/simple-user-details.component';
import {UserFiltersComponent} from './filters/user-filters.component';
import {UserListComponent} from './list/user-list.component';
import {EmptyScreenComponent} from './empty-screen/empty-screen.component';
import {TreeUserListComponent} from './tree-list/tree-user-list.component';
import {TreeUsersListComponent} from './tree-users-list/tree-users-list.component';
import {UserInfoService} from './details/sections/info/user-info.service';
import {HttpClientModule} from '@angular/common/http';
import {globalStoreProvider} from '../core/store/global.store';
import {SmartUserCommunicationComponent} from './communication/smart-user-communication.component';
import {UserGroupsResolver} from './communication/user-groups.resolver';
import {GroupNameService} from '../core/services/group-name.service';
import {UsersComparisonComponent} from './users-comparison/users-comparison.component';
import {UserOverviewComponent} from './user-overview/user-overview.component';
import {SmartUsersComparisonComponent} from './smart-users-comparison/smart-users-comparison.component';
import {UsersService} from './users.service';
import {CommunicationModule} from '../communication/communication.module';
import { UserChildrenSectionComponent } from './details/sections/children/user-children-section.component';
import { UserAafFunctionsComponent } from './details/sections/aaf-functions/user-aaf-functions-section.component';
import { UserAdministrativeSectionComponent } from './details/sections/administrative/user-administrative-section.component';
import { UserConnectionSectionComponent } from './details/sections/connection/user-connection-section.component';
import { UserInfoSectionComponent } from './details/sections/info/user-info-section.component';
import { UserRelativesSectionComponent } from './details/sections/relatives/user-relatives-section.component';
import { UserStructuresSectionComponent } from './details/sections/structures/user-structures-section.component';
import { UserDuplicatesSectionComponent } from './details/sections/duplicates/user-duplicates.section.component';
import { UserClassesSectionComponent } from './details/sections/classes/user-classes-section.component';
import { UserManualgroupsSectionComponent } from './details/sections/manualgroups/user-manualgroups-section.component';
import { UserFunctionalgroupsSectionComponent } from './details/sections/functionalgroups/user-functionalgroups-section.component';
import { UserQuotaSectionComponent } from './details/sections/quota/user-quota-section.component';
import { UserlistFiltersService } from '../core/services/userlist.filters.service';
import { UsersListComponent } from './users-list/users-list.component';
import { UsersRelinkComponent } from './users-relink/users-relink.component';
import { SharedModule } from '../_shared/shared.module';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        NgxOdeSijilModule.forChild(),
        NgxOdeUiModule,
        CommunicationModule,
        HttpClientModule,
        SharedModule
    ],
    declarations: [
        UsersComponent,
        UserCreateComponent,
        UserDetailsComponent,
        SimpleUserDetailsComponent,
        UserFiltersComponent,
        UserListComponent,
        TreeUserListComponent,
        TreeUsersListComponent,
        EmptyScreenComponent,
        UserChildrenSectionComponent,
        UserAafFunctionsComponent,
        UserAdministrativeSectionComponent,
        UserConnectionSectionComponent,
        UserInfoSectionComponent,
        UserRelativesSectionComponent,
        UserStructuresSectionComponent,
        UserDuplicatesSectionComponent,
        UserClassesSectionComponent,
        UserManualgroupsSectionComponent,
        UserFunctionalgroupsSectionComponent,
        SmartUserCommunicationComponent,
        UserQuotaSectionComponent,
        UsersComparisonComponent,
        UserOverviewComponent,
        SmartUsersComparisonComponent,
        UsersRelinkComponent,
        UsersListComponent,
    ],
    providers: [
        UserDetailsResolver,
        UsersResolver,
        RemovedUsersResolver,
        UserGroupsResolver,
        UserlistFiltersService,
        UserInfoService,
        globalStoreProvider,
        GroupNameService,
        UsersService
    ],
    exports: [
        RouterModule,
        SimpleUserDetailsComponent
    ]
})
export class UsersModule {
}
