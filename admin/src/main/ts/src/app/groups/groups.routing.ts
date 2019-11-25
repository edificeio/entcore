import {Routes} from '@angular/router';

import {GroupsComponent} from './groups/groups.component';
import {GroupCreateComponent} from './create/group-create/group-create.component';
import {GroupDetailsComponent} from './details/group-details/group-details.component';
import {GroupsTypeViewComponent} from './type-view/groups-type-view.component';
import {GroupsResolver} from './groups.resolver';
import {GroupDetailsResolver} from './details/group-details.resolver';
import {GroupInternalCommunicationRuleResolver} from './details/group-internal-communication-rule.resolver';
import {SmartGroupCommunicationComponent} from './communication/smart-group-communication/smart-group-communication.component';

export let routes: Routes = [
    {
        path: '', component: GroupsComponent, resolve: {grouplist: GroupsResolver},
        children: [
            {
                path: ':groupType', component: GroupsTypeViewComponent,
                children: [
                    {
                        path: 'create',
                        component: GroupCreateComponent
                    },
                    {
                        path: ':groupId/details',
                        component: GroupDetailsComponent,
                        resolve: {
                            group: GroupDetailsResolver,
                            rule: GroupInternalCommunicationRuleResolver
                        }
                    },
                    {
                        path: ':groupId/communication',
                        component: SmartGroupCommunicationComponent,
                        resolve: {
                            group: GroupDetailsResolver
                        }
                    }
                ]
            }]
    }
];
