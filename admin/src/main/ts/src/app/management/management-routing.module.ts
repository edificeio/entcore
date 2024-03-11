import {Routes} from '@angular/router';
import {ManagementRootComponent} from './management-root/management-root.component';
import {StructureInformationsComponent} from './structure-informations/structure-informations.component';
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
import {CalendarComponent} from './calendar/calendar.component';
import { SubjectsComponent} from './subjects/subjects.component';
import {SubjectsResolver} from './subjects/subjects.resolver';
import {SubjectCreate} from './subjects/create/subject-create.component';
import {SubjectDetails} from './subjects/details/subject-details.component';
import {ImportEdtGuardService} from './import-edt/import-edt-guard.service';
import {CalendarGuardService} from './calendar/calendar-guard.service';
import {SubjectsGuardService} from './subjects/subjects-guard.service';
import {StructureGarComponent} from './structure-gar/structure-gar.component';

export let routes: Routes = [
     {
        path: '', component: ManagementRootComponent,
        children: [
            {
                path: '',
                redirectTo: 'infos',
                pathMatch: 'full'
            },
            {
                path: 'infos',
                component: StructureInformationsComponent
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
                //canActivate : [ZimbraGuardService],
                component: ZimbraComponent
            },
            {
                path: 'import-edt',
                //canActivate : [ImportEdtGuardService],
                component: ImportEDTComponent
            },
            {
                path: 'calendar',
                //canActivate : [CalendarGuardService],
                component: CalendarComponent
            },
            {
                path: 'subjects',
                component: SubjectsComponent,
                //canActivateChild : [SubjectsGuardService],
                resolve: {
                    subjectLit: SubjectsResolver
                },
                children: [
                    {
                        path: 'create',
                        component: SubjectCreate
                    },
                    {
                        path: ':subjectId/details',
                        component: SubjectDetails
                    },
                ]
            },
            {
                path: 'gar',
                component: StructureGarComponent
            }
        ]
     }
];
