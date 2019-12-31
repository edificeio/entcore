import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {NgModule} from '@angular/core';
import {MatPaginatorModule, MatPaginatorIntl} from '@angular/material/paginator';
import {MatSortModule} from '@angular/material/sort';
import {MatTableModule} from '@angular/material/table';
import {NgxOdeSijilModule, BundlesService} from 'ngx-ode-sijil';
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
import {BlockProfilesComponent} from './block-profile/block-profiles.component';
import {BlockProfilesService} from './block-profile/block-profiles.service';
import { MatPaginatorIntlService } from './block-profile/MatPaginatorIntl.service';
import { NgxTrumbowygModule } from 'ngx-trumbowyg';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        NgxOdeUiModule,
        NgxOdeSijilModule.forChild(),
        RouterModule.forChild(routes),
        MatPaginatorModule,
        MatSortModule,
        MatTableModule,
        NgxTrumbowygModule.withConfig({
            svgPath: '/admin/public/dist/assets/trumbowyg/icons.svg',
            removeformatPasted: true,
            semantic: false,
            btns: [
                ['historyUndo', 'historyRedo'],
                ['strong', 'em', 'underline'],
                ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
                ['foreColor', 'fontfamily', 'fontsize'],
                ['link'],
                ['viewHTML']
            ]
        })
    ],
    declarations: [
        ManagementRootComponent,
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
        BlockProfilesService,
        {
            provide: MatPaginatorIntl,
            useFactory: (bundlesService) => new MatPaginatorIntlService(bundlesService),
            deps: [BundlesService]
        }
    ]
})
export class ManagementModule {}
