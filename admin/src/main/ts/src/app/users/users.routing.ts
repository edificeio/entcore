import {Routes} from '@angular/router';
import {UsersResolver, RemovedUsersResolver} from './users.resolver';
import {UserDetailsResolver} from './details/user-details.resolver';
import {UsersComponent} from './users.component';
import {UserCreateComponent} from './create/user-create.component';
import {UserFiltersComponent} from './filters/user-filters.component';
import {UserDetailsComponent} from './details/user-details.component';
import {SimpleUserDetailsComponent} from './simple-details/simple-user-details.component';
import {ConfigResolver} from '../core/resolvers/config.resolver';
import {SmartUserCommunicationComponent} from './communication/smart-user-communication.component';
import {UserGroupsResolver} from './communication/user-groups.resolver';
import {UsersRelinkComponent} from './users-relink/users-relink.component';
import { UsersListComponent } from './users-list/users-list.component';
import { TreeUsersListComponent } from './tree-users-list/tree-users-list.component';
import { EmptyScreenComponent } from './empty-screen/empty-screen.component';

export let routes: Routes = [
    {
        path: '', component: UsersComponent,
        children: [
            {
                path: 'list', component: UsersListComponent, resolve: {users: UsersResolver},
                children: [
                    {path: 'create', component: UserCreateComponent},
                    {path: 'filter', component: UserFiltersComponent},
                    {
                        path: ':userId/details', component: UserDetailsComponent, resolve: {
                            config: ConfigResolver,
                            user: UserDetailsResolver
                        }
                    },
                    {
                        path: ':userId/communication', component: SmartUserCommunicationComponent, resolve: {
                            user: UserDetailsResolver,
                            groups: UserGroupsResolver
                        }
                    },
                    {path:'',redirectTo:'filter'},
                ]
            },
            {
                path: 'tree-list', component: TreeUsersListComponent,
                children: [
                    {path: 'search', component: EmptyScreenComponent},
                    {
                        path: ':userId/details', component: SimpleUserDetailsComponent, resolve: {
                            config: ConfigResolver,
                            user: UserDetailsResolver
                        }
                    },
                    {path:'',redirectTo:'search'},
                ]
            },
            {
                path: 'relink', component: UsersRelinkComponent, resolve: {users: RemovedUsersResolver},
                children: [
                    {path: 'filter', component: UserFiltersComponent},
                    {
                        path: ':userId/details', component: UserDetailsComponent, resolve: {
                            config: ConfigResolver,
                            user: UserDetailsResolver
                        }
                    },
                    {
                        path: ':userId/communication', component: SmartUserCommunicationComponent, resolve: {
                            user: UserDetailsResolver,
                            groups: UserGroupsResolver
                        }
                    },
                    {path:'',redirectTo:'filter'},
                ]
            },
            // Redirections for compatibility with old URLs
            {path:'', redirectTo: 'list/filter'},
            {path:'create', redirectTo: 'list/create'},
            {path:'filter', redirectTo: 'list/filter'},
            {path:':userId/details', redirectTo: 'list/:userId/details'},
            {path:':userId/communication', redirectTo: 'list/:userId/communication'},
        ]
    }
];
