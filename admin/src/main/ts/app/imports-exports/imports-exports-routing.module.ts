import { Routes } from '@angular/router'
import { ImportsExportsRoot } from './imports-exports-root.component'
import { ImportCSV } from './import/import-csv.component'
import { ExportComponent } from './export/export.component'
import { MassMailComponent } from './export/mass-mail.component'


export let routes : Routes = [
     { 
        path: '', component: ImportsExportsRoot, 
        children: [
            { path: 'import-csv', component: ImportCSV },
            { path: 'export', component: ExportComponent },
            { path: 'massmail', component: MassMailComponent }
        ]
     }
   
]