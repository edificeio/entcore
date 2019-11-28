import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {NgModule} from '@angular/core';
import {SijilModule} from 'sijil';
import {UxModule} from '../shared/ux/ux.module';
import {routes} from './users.routing';
import {UserDetailsResolver} from './details/user-details.resolver';
import {UsersResolver} from './users.resolver';
import {UsersComponent} from './users.component';
import {UserCreateComponent} from './create/user-create.component';
import {UserDetailsComponent} from './details/user-details.component';
import {UserFiltersComponent} from './filters/user-filters.component';
import {UserListComponent} from './list/user-list.component';
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
import { UserInfoSectionComponent } from './details/sections/info/user-info-section.component';
import { UserRelativesSectionComponent } from './details/sections/relatives/user-relatives-section.component';
import { UserStructuresSectionComponent } from './details/sections/structures/user-structures-section.component';
import { UserDuplicatesSectionComponent } from './details/sections/duplicates/user-duplicates.section.component';
import { UserClassesSectionComponent } from './details/sections/classes/user-classes-section.component';
import { UserManualgroupsSectionComponent } from './details/sections/manualgroups/user-manualgroups-section.component';
import { UserFunctionalgroupsSectionComponent } from './details/sections/functionalgroups/user-functionalgroups-section.component';
import { UserQuotaSectionComponent } from './details/sections/quota/user-quota-section.component';
import { UserlistFiltersService } from '../core/services/userlist.filters.service';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        SijilModule.forChild(),
        UxModule,
        CommunicationModule,
        HttpClientModule
    ],
    declarations: [
        UsersComponent,
        UserCreateComponent,
        UserDetailsComponent,
        UserFiltersComponent,
        UserListComponent,
        UserChildrenSectionComponent,
        UserAafFunctionsComponent,
        UserAdministrativeSectionComponent,
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
        SmartUsersComparisonComponent
    ],
    providers: [
        UserDetailsResolver,
        UsersResolver,
        UserGroupsResolver,
        UserlistFiltersService,
        UserInfoService,
        globalStoreProvider,
        GroupNameService,
        UsersService
    ],
    exports: [
        RouterModule
    ]
})
export class UsersModule {
}
