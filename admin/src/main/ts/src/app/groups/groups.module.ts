import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {NgModule} from '@angular/core';
import {SijilModule} from 'sijil';

import {UxModule} from '../shared/ux/ux.module';
import {routes} from './groups.routing';
import {GroupsResolver} from './groups.resolver';
import {GroupDetailsResolver} from './details/group-details.resolver';
import {GroupInternalCommunicationRuleResolver} from './details/group-internal-communication-rule.resolver';
import {GroupsStore} from './groups.store';
import {GroupNameService} from '../core/services/group-name.service';
import {UserlistFiltersService} from '../core/services/userlist.filters.service';

import {GroupsComponent} from './groups/groups.component';
import {GroupCreateComponent} from './create/group-create/group-create.component';
import {GroupDetailsComponent} from './details/group-details/group-details.component';
import {GroupManageUsersComponent} from './details/manage-users/group-manage-users/group-manage-users.component';
import {GroupInputUsersComponent} from './details/manage-users/input/group-input-users/group-input-users.component';
import {GroupInputFiltersComponent} from './details/manage-users/input/group-input-filters/group-input-filters.component';
import {GroupOutputUsersComponent} from './details/manage-users/output/group-output-users/group-output-users.component';
import {GroupUsersListComponent} from './details/users-list/group-users-list.component';
import {GroupsTypeViewComponent} from './type-view/groups-type-view.component';
import {GroupsService} from './groups.service';
import {CommunicationModule} from '../communication/communication.module';
import {SmartGroupCommunicationComponent} from './communication/smart-group-communication/smart-group-communication.component';
import {globalStoreProvider} from '../core/store/global.store';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        UxModule,
        CommunicationModule,
        SijilModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        GroupsComponent,
        GroupCreateComponent,
        GroupDetailsComponent,
        GroupManageUsersComponent,
        GroupInputUsersComponent,
        GroupInputFiltersComponent,
        GroupOutputUsersComponent,
        GroupUsersListComponent,
        GroupsTypeViewComponent,
        SmartGroupCommunicationComponent
    ],
    providers: [
        GroupsResolver,
        GroupDetailsResolver,
        GroupInternalCommunicationRuleResolver,
        GroupsStore,
        UserlistFiltersService,
        GroupNameService,
        GroupsService,
        globalStoreProvider
    ],
    exports: [
        RouterModule
    ]
})
export class GroupsModule {
}
