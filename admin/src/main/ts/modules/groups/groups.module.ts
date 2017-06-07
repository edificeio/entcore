import { InfraComponentsModule } from 'infra-components'
import { CommonModule } from '@angular/common'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { NgModule } from '@angular/core'

import { SijilModule } from 'sijil'
import { UxModule } from '..'
import { routes } from './routing/routes'
import {
    GroupsRoot,
    GroupDetail,
    GroupManageUsers,
    GroupInputUsers,
    GroupInputFiltersUsers,
    GroupOutputUsers,
    GroupUsersList,
    GroupsTypeView,
    GroupCreate } from './components'
import { GroupResolve, GroupsResolve } from './routing'
import { GroupsStore } from './store'
import { UserlistFiltersService } from '../../services'

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        UxModule,
        SijilModule.forChild(),
        InfraComponentsModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        GroupsRoot,
        GroupDetail,
        GroupManageUsers,
        GroupInputUsers,
        GroupInputFiltersUsers,
        GroupOutputUsers,
        GroupUsersList,
        GroupsTypeView,
        GroupCreate
    ],
    providers: [
        GroupsStore,
        GroupResolve,
        GroupsResolve,
        UserlistFiltersService
    ],
    exports: [
        RouterModule
    ]
})
export class GroupsModule {}
