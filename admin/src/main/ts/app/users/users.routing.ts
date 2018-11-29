import { Routes } from '@angular/router';

import { UsersResolver } from './users.resolver';
import { UserDetailsResolver } from './details/user-details.resolver';

import { UsersComponent } from './users.component';
import { UserCreate } from './create/user-create.component';
import { UserFilters } from './filters/user-filters.component';
import { UserDetails } from './details/user-details.component';
import { ConfigResolver } from './details/config.resolver';

export let routes: Routes = [
    {
        path: '', component: UsersComponent, resolve: {users: UsersResolver},
        children: [
            {path: 'create', component: UserCreate},
            {path: 'filter', component: UserFilters},
            {
                path: ':userId', component: UserDetails, resolve: {
                    config: ConfigResolver,
                    user: UserDetailsResolver
                }
            }
        ]
    }
];
