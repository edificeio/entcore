import { CommonModule } from '@angular/common'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { NgModule } from '@angular/core'
import { SijilModule } from 'sijil'
import { UxModule } from '../shared/ux/ux.module'
import { routes } from './management-routing.module'
import { ManagementRoot } from './management-root.component'
import { MessageFlashComponent } from './message-flash/message-flash.component'
import { EditMessageFlashComponent } from './message-flash/form/edit-message-flash.component'
import { DuplicateMessageFlashComponent } from './message-flash/form/duplicate-message-flash.component'
import { CreateMessageFlashComponent } from './message-flash/form/create-message-flash.component'
import { MessageFlashFormComponent } from './message-flash/form/message-flash-form.component'
import { MessageFlashStore } from './message-flash/message-flash.store'
import { MessageFlashResolver } from './message-flash/message-flash.resolver'


@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        UxModule,
        SijilModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        ManagementRoot,
        MessageFlashComponent,
        EditMessageFlashComponent,
        DuplicateMessageFlashComponent,
        CreateMessageFlashComponent,
        MessageFlashFormComponent
    ],
    exports: [
        RouterModule
    ],
    providers: [
        MessageFlashStore,
        MessageFlashResolver
    ]
})
export class ManagementModule {}