import { Routes } from '@angular/router'

import { GroupsComponent } from './groups.component'
import { GroupCreate } from './create/group-create.component'
import { GroupDetails } from './details/group-details.component'
import { GroupsTypeView } from './type-view/groups-type-view.component'
import { GroupsResolver } from './groups.resolver'
import { GroupDetailsResolve } from './details/group-details.resolve';

export let routes : Routes = [
    { 
        path: '', component: GroupsComponent, resolve: { grouplist: GroupsResolver }, 
        children: [
            { 
                path: ':groupType', component: GroupsTypeView,
                children: [
                    { path: 'create',   component: GroupCreate },
                    { path: ':groupId', component: GroupDetails, resolve: { _: GroupDetailsResolve } }
                ]
            }]
    }
]