import {Routes} from '@angular/router';
import {ImportsExportsRootComponent} from './imports-exports-root/imports-exports-root.component';
import {ImportCSVComponent} from './import/import-csv/import-csv.component';
import {ExportComponent} from './export/export.component';
import {MassMailComponent} from './mailing/mass-mail/mass-mail.component';


export let routes: Routes = [
     {
        path: '', component: ImportsExportsRootComponent,
        children: [
            { path: 'import-csv', component: ImportCSVComponent },
            { path: 'export', component: ExportComponent },
            { path: 'massmail', component: MassMailComponent }
        ]
     }

];
