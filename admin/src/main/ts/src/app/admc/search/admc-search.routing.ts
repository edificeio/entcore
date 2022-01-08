import { Routes } from '@angular/router';
import { AdmcSearchComponent } from './admc-search.component';
import { AdmcSearchTransverseComponent } from './transverse/admc-search-transverse.component';

export let routes: Routes = [
    {
        path: '', 
        component: AdmcSearchComponent,
        children: [
            {
                path: 'transverse',
                component: AdmcSearchTransverseComponent
            }
        ]
    }
];