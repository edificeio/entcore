import { Routes } from '@angular/router';
import { UsersResolver } from './users.resolver';
import { UserDetailsResolver } from './details/user-details.resolver';
import { UsersComponent } from './users.component';
import { UserCreate } from './create/user-create.component';
import { UserFilters } from './filters/user-filters.component';
import { UserDetails } from './details/user-details.component';
import { ConfigResolver } from '../core/resolvers/config.resolver';
import { SmartUserCommunicationComponent } from './communication/smart-user-communication.component';
import { UserGroupsResolver } from './communication/user-groups.resolver';

export let routes: Routes = [
    {
        path: '', component: UsersComponent, resolve: {users: UsersResolver},
        children: [
            {path: 'create', component: UserCreate},
            {path: 'filter', component: UserFilters},
            {
                path: ':userId/details', component: UserDetails, resolve: {
                    config: ConfigResolver,
                    user: UserDetailsResolver
                }
            },
            {
                path: ':userId/communication', component: SmartUserCommunicationComponent, resolve: {
                    user: UserDetailsResolver,
                    groups: UserGroupsResolver
                }
            }
        ]
    }
];
