import { Routes } from '@angular/router';
import { AdmcAppsComponent } from './admc-apps.component';

export let routes: Routes = [
    {
        path: '', 
        component: AdmcAppsComponent,
        children: [
            {
                path: 'roles', loadChildren: () => import('./roles/admc-apps-roles.module').then(m => m.AdmcAppsRolesModule)
            }
        ]
    }
];