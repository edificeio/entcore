import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {NgModule} from '@angular/core';
import {NgxOdeSijilModule} from 'ngx-ode-sijil';
import {NgxOdeUiModule} from 'ngx-ode-ui';
import {routes} from './imports-exports.routing';
import {ImportsExportsRootComponent} from './imports-exports-root/imports-exports-root.component';
import {ImportCSVComponent} from './import/import-csv/import-csv.component';
import {MappingsTableComponent} from './import/mappings-table/mappings-table.component';
import {ExportComponent} from './export/export.component';
import {MassMailComponent} from './mailing/mass-mail/mass-mail.component';
import {UserlistFiltersService} from '../core/services/userlist.filters.service';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        NgxOdeUiModule,
        NgxOdeSijilModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        ImportsExportsRootComponent,
        ImportCSVComponent,
        MappingsTableComponent,
        ExportComponent,
        MassMailComponent
    ],
    providers: [
        UserlistFiltersService,
    ],
    exports: [
        RouterModule
    ]
})
export class ImportsExportsModule {}
