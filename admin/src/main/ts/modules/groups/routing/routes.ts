import { Routes } from '@angular/router'
import { GroupsRoot, GroupsTypeView, GroupDetailComponent } from '../components'
import { GroupResolve, GroupsResolve } from './index'

export let routes : Routes = [
    { path: '', component: GroupsRoot, resolve: { grouplist: GroupsResolve }, children: [
        { path: ':groupType', component: GroupsTypeView,
            children: [
                { path: ':groupId', component: GroupDetailComponent, resolve: { _: GroupResolve } }
            ]
        }]
    }
]