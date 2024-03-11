import { Routes } from '@angular/router';
import { AdmcMiscComponent } from './admc-misc.component';
import { GarComponent } from './gar/gar.component';

export let routes: Routes = [
    {
        path: '', 
        component: AdmcMiscComponent,
        children: [
            {
                path: 'gar', component: GarComponent
            }
        ]
    }
];