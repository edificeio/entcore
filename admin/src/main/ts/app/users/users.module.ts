import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpModule } from '@angular/http';
import { NgModule } from '@angular/core';
import { SijilModule } from 'sijil';

import { UxModule } from '../shared/ux/ux.module';
import { routes } from './users.routing';
import { UserDetailsResolver } from './details/user-details.resolver';
import { ConfigResolver } from './details/config.resolver';
import { UsersResolver } from './users.resolver';
import { UsersComponent } from './users.component';
import { UserCreate } from './create/user-create.component';
import { UserDetails } from './details/user-details.component';
import { UserFilters } from './filters/user-filters.component';
import { UserList } from './list/user-list.component';
import {
    UserAafFunctionsComponent,
    UserAdministrativeSection,
    UserChildrenSection,
    UserClassesSection,
    UserDuplicatesSection,
    UserFunctionalGroupsSection,
    UserInfoSection,
    UserManualGroupsSection,
    UserQuotaSection,
    UserRelativesSection,
    UserStructuresSection
} from './details/sections';
import { UserlistFiltersService } from '../core/services';
import { UserInfoService } from './details/sections/info/user-info.service';
import { HttpClientModule } from '@angular/common/http';
import { globalStoreProvider } from '../core/store';
import { UserCommunicationComponent } from './communication/user-communication.component';
import { SmartUserCommunicationComponent } from './communication/smart-user-communication.component';
import { GroupCardComponent } from './communication/group-card.component';
import { CommunicationRulesComponent } from './communication/communication-rules.component';
import { UserGroupsResolver } from './communication/user-groups.resolver';
import { GroupNameService } from '../core/services/group-name.service';
import { UsersComparisonComponent } from './users-comparison.component';
import { UserOverviewComponent } from './user-overview.component';
import { SmartUsersComparisonComponent } from './smart-users-comparison.component';
import { UserService } from "./user.service";

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        HttpModule,
        RouterModule.forChild(routes),
        SijilModule.forChild(),
        UxModule,
        HttpClientModule
    ],
    declarations: [
        UsersComponent,
        UserCreate,
        UserDetails,
        UserFilters,
        UserList,
        UserChildrenSection,
        UserAafFunctionsComponent,
        UserAdministrativeSection,
        UserInfoSection,
        UserRelativesSection,
        UserStructuresSection,
        UserDuplicatesSection,
        UserClassesSection,
        UserManualGroupsSection,
        UserFunctionalGroupsSection,
        UserCommunicationComponent,
        SmartUserCommunicationComponent,
        UserQuotaSection,
        GroupCardComponent,
        CommunicationRulesComponent,
        UsersComparisonComponent,
        UserOverviewComponent,
        SmartUsersComparisonComponent
    ],
    providers: [
        ConfigResolver,
        UserDetailsResolver,
        UsersResolver,
        UserGroupsResolver,
        UserlistFiltersService,
        UserInfoService,
        globalStoreProvider,
        GroupNameService,
        UserService
    ],
    exports: [
        RouterModule
    ]
})
export class UsersModule {
}
