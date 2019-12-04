import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {NgModule} from '@angular/core';
import {NgxOdeSijilModule} from 'ngx-ode-sijil';
import {NgxOdeUiModule} from 'ngx-ode-ui';
import {routes} from './management-routing.module';
import {ManagementRootComponent} from './management-root/management-root.component';
import {MessageFlashComponent} from './message-flash/message-flash.component';
import {MessageFlashListComponent} from './message-flash/message-flash-list/message-flash-list.component';
import {EditMessageFlashComponent} from './message-flash/form/edit-message-flash.component';
import {DuplicateMessageFlashComponent} from './message-flash/form/duplicate-message-flash.component';
import {CreateMessageFlashComponent} from './message-flash/form/create-message-flash.component';
import {MessageFlashFormComponent} from './message-flash/form/message-flash-form/message-flash-form.component';
import {MessageFlashPreviewComponent} from './message-flash/form/message-flash-preview/message-flash-preview.component';
import {MessageFlashStore} from './message-flash/message-flash.store';
import {MessageFlashResolver} from './message-flash/message-flash.resolver';


@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        NgxOdeUiModule,
        NgxOdeSijilModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        ManagementRootComponent,
        MessageFlashComponent,
        MessageFlashListComponent,
        EditMessageFlashComponent,
        DuplicateMessageFlashComponent,
        CreateMessageFlashComponent,
        MessageFlashFormComponent,
        MessageFlashPreviewComponent
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
