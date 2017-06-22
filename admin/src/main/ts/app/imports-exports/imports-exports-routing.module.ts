import { Routes } from '@angular/router'
import { ImportsExportsRoot } from './imports-exports-root.component'
import { ImportCSV } from './import/import-csv.component'


export let routes : Routes = [
     { 
        path: '', component: ImportsExportsRoot, 
        children: [
            { path: 'import-csv', component: ImportCSV }
        ]
     }
   
]