import { InfraComponentsModule } from 'infra-components/dist'
import { CommonModule } from '@angular/common'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { NgModule } from '@angular/core'

import { SijilModule } from 'sijil'
import { UxModule } from '..'
import { routes } from './routing/routes'
//import { declarations, providers } from './module.properties'
import { GroupDetailComponent, GroupsRoot, GroupsTypeView, GroupUsersList } from './components'
import { GroupResolve, GroupsResolve } from './routing'
import { GroupsStore } from './store'

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
        GroupDetailComponent,
        GroupUsersList,
        GroupsRoot,
        GroupsTypeView
    ],
    providers: [
        GroupsStore,
        GroupResolve,
        GroupsResolve
    ],
    exports: [
        RouterModule
    ]
})
export class GroupsModule {}