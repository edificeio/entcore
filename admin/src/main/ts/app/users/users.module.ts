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
UserAdministrativeSection,
UserChildrenSection,
UserClassesSection,
UserDuplicatesSection,
UserFunctionalGroupsSection,
UserInfoSection,
UserManualGroupsSection,
UserRelativesSection,
UserStructuresSection
} from './details/sections';
import { UserlistFiltersService } from '../core/services';
import { UserInfoService } from './details/sections/info/user-info.service';
import { HttpClientModule } from '@angular/common/http';

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
        UserAdministrativeSection,
        UserInfoSection,
        UserRelativesSection,
        UserStructuresSection,
        UserDuplicatesSection,
        UserClassesSection,
        UserManualGroupsSection,
        UserFunctionalGroupsSection
    ],
    providers: [
        ConfigResolver,
        UserDetailsResolver,
        UsersResolver,
        UserlistFiltersService,
        UserInfoService
    ],
    exports: [
        RouterModule
    ]
})
export class UsersModule {
}
