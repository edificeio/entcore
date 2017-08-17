import { Routes } from '@angular/router'

import { UsersResolve } from './users.resolve'
import { UserDetailsResolve } from './details/user-details.resolve'

import { UsersComponent } from './users.component'
import { UserCreate } from './create/user-create.component';
import { UserFilters } from './filters/user-filters.component';
import { UserDetails } from './details/user-details.component';

export let routes : Routes = [
    { 
        path: '', component: UsersComponent, resolve: { userlist: UsersResolve },
        children: [
            { path: 'create', 	component: UserCreate },
            { path: 'filter', 	component: UserFilters },
            { path: ':userId', 	component: UserDetails, resolve: { user: UserDetailsResolve }}
        ]
    }
]