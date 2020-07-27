import {Routes} from '@angular/router';
import {ManagementRootComponent} from './management-root/management-root.component';
import {MessageFlashComponent} from './message-flash/message-flash.component';
import {MessageFlashListComponent} from './message-flash/message-flash-list/message-flash-list.component';
import {EditMessageFlashComponent} from './message-flash/form/edit-message-flash.component';
import {DuplicateMessageFlashComponent} from './message-flash/form/duplicate-message-flash.component';
import {CreateMessageFlashComponent} from './message-flash/form/create-message-flash.component';
import {MessageFlashResolver} from './message-flash/message-flash.resolver';
import { BlockProfilesComponent } from './block-profile/block-profiles.component';
import { ZimbraComponent } from './zimbra/zimbra.component';
import { ImportEDTComponent } from './import-edt/import-edt.component';
import {ZimbraGuardService} from './zimbra/zimbra-guard.service';
import {CalendarComponent} from "./calendar/calendar.component";

export let routes: Routes = [
     {
        path: '', component: ManagementRootComponent,
        children: [
            {
                path: '',
                redirectTo: 'message-flash',
                pathMatch: 'full'
            },
            {
                path: 'message-flash',
                component: MessageFlashComponent,
                children: [
                    {
                        path: 'list',
                        component: MessageFlashListComponent,
                        resolve: {
                            messages: MessageFlashResolver
                        }
                    },
                    {
                        path: 'edit/:messageId',
                        component: EditMessageFlashComponent,
                        resolve: {
                            messages: MessageFlashResolver
                        }
                    },
                    {
                        path: 'duplicate/:messageId',
                        component: DuplicateMessageFlashComponent,
                        resolve: {
                            messages: MessageFlashResolver
                        }
                    },
                    {
                        path: 'create',
                        component: CreateMessageFlashComponent
                    }
                ]
            },
            {
                path: 'block-profiles',
                component: BlockProfilesComponent
            },
            {
                path: 'zimbra',
                canActivate : [ZimbraGuardService],
                component: ZimbraComponent
            },
            {
                path: 'import-edt',
                component: ImportEDTComponent
            },
            {
                path: 'calendar',
                component: CalendarComponent
            }
        ]
     }
];
