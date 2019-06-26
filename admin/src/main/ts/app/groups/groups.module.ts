import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { SijilModule } from 'sijil';

import { UxModule } from '../shared/ux/ux.module';
import { routes } from './groups.routing';
import { GroupsResolver } from './groups.resolver';
import { GroupDetailsResolver } from './details/group-details.resolver';
import { GroupInternalCommunicationRuleResolver } from './details/group-internal-communication-rule.resolver';
import { GroupsStore } from './groups.store';
import { GroupNameService, UserlistFiltersService } from '../core/services';

import { GroupsComponent } from './groups.component';
import { GroupCreate } from './create/group-create.component';
import { GroupDetails } from './details/group-details.component';
import { GroupManageUsers } from './details/manage-users/group-manage-users.component';
import { GroupInputUsers } from './details/manage-users/input/group-input-users.component';
import { GroupInputFilters } from './details/manage-users/input/group-input-filters.component';
import { GroupOutputUsers } from './details/manage-users/output/group-output-users.component';
import { GroupUsersList } from './details/users-list/group-users-list.component';
import { GroupsTypeView } from './type-view/groups-type-view.component';
import { GroupsService } from './groups.service';
import { CommunicationModule } from '../communication/communication.module';
import { SmartGroupCommunicationComponent } from './communication/smart-group-communication.component';
import { globalStoreProvider } from '../core/store';

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
        GroupCreate,
        GroupDetails,
        GroupManageUsers,
        GroupInputUsers,
        GroupInputFilters,
        GroupOutputUsers,
        GroupUsersList,
        GroupsTypeView,
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
