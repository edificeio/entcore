import { InfraComponentsModule } from 'infra-components/dist'
import { CommonModule } from '@angular/common'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { NgModule } from '@angular/core'

import { SijilModule } from 'sijil'
import { UxModule } from '..'
import { UserDetail, UsersRoot, UserList, UserFilters, UserError, UserCreate,
    UserChildrenSection, UserAdministrativeSection, UserInfoSection, UserRelativesSection,
    UserStructuresSection, UserDuplicatesSection } from './components'
import { UserResolve, UsersResolve, routes } from './routing'
import { UsersStore } from './store'

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        UxModule,
        SijilModule.forChild(),
        InfraComponentsModule,
        RouterModule.forChild(routes)
    ],
    declarations: [
        UserCreate,
        UserDetail,
        UserError,
        UserFilters,
        UserList,
        UsersRoot,
        UserChildrenSection,
        UserAdministrativeSection,
        UserInfoSection,
        UserRelativesSection,
        UserStructuresSection,
        UserDuplicatesSection
    ],
    providers: [
        UserResolve,
        UsersResolve,
        UsersStore
    ],
    exports: [
        RouterModule
    ]
})
export class UsersModule {}