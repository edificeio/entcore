import { Routes } from '@angular/router'
import { GroupsRoot, GroupsTypeView, GroupDetail, GroupCreate } from '../components'
import { GroupResolve, GroupsResolve } from './index'

export let routes : Routes = [
    { path: '', component: GroupsRoot, resolve: { grouplist: GroupsResolve }, 
        children: [
            { path: ':groupType', component: GroupsTypeView,
                children: [
                    { path: 'create',   component: GroupCreate },
                    { path: ':groupId', component: GroupDetail, resolve: { _: GroupResolve } }
                ]
            }]
    }
]