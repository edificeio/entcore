import { Routes } from "@angular/router";

import { GroupsComponent } from "./groups/groups.component";
import { GroupCreateComponent } from "./create/group-create/group-create.component";
import { GroupDetailsComponent } from "./details/group-details/group-details.component";
import { GroupsTypeViewComponent } from "./type-view/groups-type-view.component";
import { GroupsResolver } from "./groups.resolver";
import { GroupDetailsResolver } from "./details/group-details.resolver";
import { GroupInternalCommunicationRuleResolver } from "./details/group-internal-communication-rule.resolver";
import { SmartGroupCommunicationComponent } from "./communication/smart-group-communication/smart-group-communication.component";
import { GroupInfoComponent } from "./info/group-info.component";
import { ClassesComponent } from "./classes/classes.component";
import { ClassDetailsComponent } from "./classes/details/class-details.component";
import { ClassCreateComponent } from "./classes/create/class-create.component";

export let routes: Routes = [
  {
    path: "",
    component: GroupsComponent,
    children: [
      {
        path: "classes",
        component: ClassesComponent,
        children: [
          {
            path: "create",
            component: ClassCreateComponent,
          },
          {
            path: ":classId/details",
            component: ClassDetailsComponent
          }
        ]
      },
      {
        path: ":groupType",
        component: GroupsTypeViewComponent,
        resolve: { grouplist: GroupsResolver },
        children: [
          {
            path: "create",
            component: GroupCreateComponent,
          },
          {
            path: ":groupId/details",
            component: GroupDetailsComponent,
            resolve: {
              group: GroupDetailsResolver,
              rule: GroupInternalCommunicationRuleResolver,
            },
          },
          {
            path: ":groupId/communication",
            component: SmartGroupCommunicationComponent,
            resolve: {
              group: GroupDetailsResolver,
            },
          },
        ],
      },
    ],
  },
];
