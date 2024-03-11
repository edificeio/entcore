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
import {StructureInformationsComponent} from './structure-informations/structure-informations.component';
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
import {ZimbraComponent} from './zimbra/zimbra.component';
import {ImportEDTComponent} from './import-edt/import-edt.component';
import {SubjectsComponent} from './subjects/subjects.component';
import { MatPaginatorIntlService } from './block-profile/MatPaginatorIntl.service';
import { NgxTrumbowygModule } from 'ngx-trumbowyg';
import {SubjectCreate} from './subjects/create/subject-create.component';
import {SubjectDetails} from './subjects/details/subject-details.component';
import {SubjectsResolver} from './subjects/subjects.resolver';
import {SubjectsStore} from './subjects/subjects.store';
import {SubjectsService} from './subjects/subjects.service';
import {CalendarComponent} from './calendar/calendar.component';
import {MatRadioModule} from '@angular/material/radio';
import {MatDividerModule} from '@angular/material/divider';
import {FlexLayoutModule} from '@angular/flex-layout';
import {ZimbraService} from './zimbra/zimbra.service';
import {CalendarService} from './calendar/calendar.service';
import {ImportEDTReportsService} from './import-edt/import-edt-reports.service';
import {SubjectsGuardService} from './subjects/subjects-guard.service';
import { StructureAttachmentComponent } from './structure-attachment/structure-attachment.component';
import {StructureGarComponent} from './structure-gar/structure-gar.component';

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
        }),
        MatRadioModule,
        MatDividerModule,
        FlexLayoutModule
    ],
    declarations: [
        ZimbraComponent,
        ManagementRootComponent,
        StructureInformationsComponent,
        MessageFlashComponent,
        MessageFlashListComponent,
        EditMessageFlashComponent,
        DuplicateMessageFlashComponent,
        CreateMessageFlashComponent,
        MessageFlashFormComponent,
        MessageFlashPreviewComponent,
        BlockProfilesComponent,
        ImportEDTComponent,
        SubjectsComponent,
        SubjectCreate,
        SubjectDetails,
        CalendarComponent,
        StructureAttachmentComponent,
        StructureGarComponent

    ],
    exports: [
        RouterModule
    ],
    providers: [
        MessageFlashStore,
        MessageFlashResolver,
        BlockProfilesService,
        ZimbraService,
        {
            provide: MatPaginatorIntl,
            useFactory: (bundlesService) => new MatPaginatorIntlService(bundlesService),
            deps: [BundlesService]
        },
        SubjectsResolver,
        SubjectsStore,
        SubjectsService,
        CalendarService,
        ImportEDTReportsService,
        SubjectsGuardService
    ]
})
export class ManagementModule {}
