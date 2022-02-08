import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";
import { NgModule } from "@angular/core";
import { NgxOdeSijilModule } from "ngx-ode-sijil";

import { NgxOdeUiModule } from "ngx-ode-ui";
import { routes } from "./groups.routing";
import { GroupsResolver } from "./groups.resolver";
import { GroupDetailsResolver } from "./details/group-details.resolver";
import { GroupInternalCommunicationRuleResolver } from "./details/group-internal-communication-rule.resolver";
import { GroupsStore } from "./groups.store";
import { GroupNameService } from "../core/services/group-name.service";
import { UserlistFiltersService } from "../core/services/userlist.filters.service";

import { GroupsComponent } from "./groups/groups.component";
import { GroupCreateComponent } from "./create/group-create/group-create.component";
import { GroupDetailsComponent } from "./details/group-details/group-details.component";
import { GroupManageUsersComponent } from "./details/manage-users/group-manage-users/group-manage-users.component";
import { GroupInputUsersComponent } from "./details/manage-users/input/group-input-users/group-input-users.component";
import { GroupInputFiltersComponent } from "./details/manage-users/input/group-input-filters/group-input-filters.component";
import { GroupOutputUsersComponent } from "./details/manage-users/output/group-output-users/group-output-users.component";
import { GroupUsersListComponent } from "./details/users-list/group-users-list.component";
import { GroupsTypeViewComponent } from "./type-view/groups-type-view.component";
import { GroupAutolinkComponent } from "./details/group-details/autolink/group-autolink.component";
import { GroupsService } from "./groups.service";
import { CommunicationModule } from "../communication/communication.module";
import { SmartGroupCommunicationComponent } from "./communication/smart-group-communication/smart-group-communication.component";
import { globalStoreProvider } from "../core/store/global.store";
import { GroupInfoComponent } from "./info/group-info.component";
import { UsersService } from "../users/users.service";

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    NgxOdeUiModule,
    CommunicationModule,
    NgxOdeSijilModule.forChild(),
    RouterModule.forChild(routes),
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
    GroupInfoComponent,
    SmartGroupCommunicationComponent,
    GroupAutolinkComponent
  ],
  providers: [
    GroupsResolver,
    GroupDetailsResolver,
    GroupInternalCommunicationRuleResolver,
    GroupsStore,
    UserlistFiltersService,
    GroupNameService,
    GroupsService,
    UsersService,
    globalStoreProvider,
  ],
  exports: [RouterModule],
})
export class GroupsModule {}
