import { CommonModule } from '@angular/common'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { NgModule } from '@angular/core'
import { InfraComponentsModule } from 'infra-components'
import { SijilModule } from 'sijil'

import { UxModule } from '../shared/ux/ux.module'
import { routes } from './users.routing'
import { UserDetailsResolve } from './details/user-details.resolve'
import { UsersResolver } from './users.resolver'
import { UsersStore } from './users.store'
import { UsersComponent } from './users.component'
import { UserCreate } from './create/user-create.component'
import { UserDetails } from './details/user-details.component'
import { UserFilters } from './filters/user-filters.component'
import { UserList } from './list/user-list.component'
import { 
    UserChildrenSection, 
    UserAdministrativeSection, 
    UserInfoSection, 
    UserRelativesSection,
    UserStructuresSection, 
    UserDuplicatesSection, 
    UserClassesSection, 
    UserManualGroupsSection, 
    UserFunctionalGroupsSection } from './details/sections'
import { UserlistFiltersService } from '../core/services'

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        InfraComponentsModule.forChild(),
        SijilModule.forChild(),
        UxModule,
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
        UserDetailsResolve,
        UsersResolver,
        UserlistFiltersService
    ],
    exports: [
        RouterModule
    ]
})
export class UsersModule {}