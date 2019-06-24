import { Routes } from '@angular/router'
import { ManagementRoot } from './management-root.component'
import { MessageFlashComponent } from './message-flash/message-flash.component'
import { EditMessageFlashComponent } from './message-flash/form/edit-message-flash.component'
import { DuplicateMessageFlashComponent } from './message-flash/form/duplicate-message-flash.component'
import { CreateMessageFlashComponent } from './message-flash/form/create-message-flash.component'
import { MessageFlashResolver } from './message-flash/message-flash.resolver';


export let routes : Routes = [
     { 
        path: '', component: ManagementRoot,
        children: [
            {
                path: '',
                redirectTo: 'message-flash',
                pathMatch: 'full'
            },
            { 
                path: 'message-flash',
                component: MessageFlashComponent,
                resolve: {
                    messages: MessageFlashResolver
                }
            },
            {
                path: 'message-flash-edit/:messageId',
                component: EditMessageFlashComponent,
                resolve: {
                    messages: MessageFlashResolver
                }
            },
            {
                path: 'message-flash-duplicate/:messageId',
                component: DuplicateMessageFlashComponent,
                resolve: {
                    messages: MessageFlashResolver
                }
            },
            {
                path: 'message-flash-create',
                component: CreateMessageFlashComponent,
                resolve: {
                    messages: MessageFlashResolver
                }
            }
        ]
     }  
]