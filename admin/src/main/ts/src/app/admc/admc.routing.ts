import { Routes } from '@angular/router';
import { AdmcComponent } from './admc.component';
import { AdmcHomeComponent } from './dashboard/admc-dashboard.component';

export let routes: Routes = [
    {
        path: '', 
        component: AdmcComponent, 
        children: [
            {
                path: 'dashboard',
                component: AdmcHomeComponent
            },
            {
                path: 'communications',
                loadChildren: () => import('./communications/admc-communications.module').then(m => m.AdmcCommunicationsModule)
            },
            {
                path: 'search',
                loadChildren: () => import('./search/admc-search.module').then(m => m.AdmcSearchModule)
            },
            {
                path: 'apps', 
                loadChildren: () => import('./apps/admc-apps.module').then(m => m.AdmcAppsModule)
            },
            {
                path: 'misc',
                loadChildren: () => import('./misc/admc-misc.module').then(m => m.AdmcMiscModule)
            },
        ]
    }
];