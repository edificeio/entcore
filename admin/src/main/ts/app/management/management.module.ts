import { CommonModule } from '@angular/common'
import { FormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { NgModule } from '@angular/core'
import { SijilModule } from 'sijil'
import { UxModule } from '../shared/ux/ux.module'
import { routes } from './management.routing'
import { ManagementRoot } from './management-root.component'
import { MessageFlashComponent } from './message-flash/message-flash.component'
import { MessageFlashListComponent } from './message-flash/message-flash-list.component'
import { EditMessageFlashComponent } from './message-flash/form/edit-message-flash.component'
import { DuplicateMessageFlashComponent } from './message-flash/form/duplicate-message-flash.component'
import { CreateMessageFlashComponent } from './message-flash/form/create-message-flash.component'
import { MessageFlashFormComponent } from './message-flash/form/message-flash-form.component'
import { MessageFlashPreviewComponent } from './message-flash/form/message-flash-preview.component'
import { MessageFlashStore } from './message-flash/message-flash.store'
import { MessageFlashResolver } from './message-flash/message-flash.resolver'
import { BlockProfilesComponent } from './block-profile/block-profiles.component';
import { BlockProfilesService } from './block-profile/block-profiles.service';

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
        MessageFlashListComponent,
        EditMessageFlashComponent,
        DuplicateMessageFlashComponent,
        CreateMessageFlashComponent,
        MessageFlashFormComponent,
        MessageFlashPreviewComponent,
        BlockProfilesComponent
    ],
    exports: [
        RouterModule
    ],
    providers: [
        MessageFlashStore,
        MessageFlashResolver,
        BlockProfilesService
    ]
})
export class ManagementModule {}