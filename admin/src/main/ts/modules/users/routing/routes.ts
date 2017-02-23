import { Routes } from '@angular/router'
import { UsersResolve, UserResolve } from './index'
import { UsersRoot, UserCreate, UserFilters, UserError, UserDetail } from '../components'

export let routes : Routes = [
    { path: '', component: UsersRoot, resolve: { userlist: UsersResolve },
        children: [
            { path: 'create', 	component: UserCreate },
            { path: 'filter', 	component: UserFilters },
            { path: 'error', 	component: UserError },
            { path: ':userId', 	component: UserDetail, resolve: { user: UserResolve }}
        ]
    }
]