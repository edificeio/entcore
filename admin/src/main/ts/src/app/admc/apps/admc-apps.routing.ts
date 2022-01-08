import { Routes } from '@angular/router';
import { AdmcAppsComponent } from './admc-apps.component';
import { AdmcAppsRolesComponent } from './roles/admc-apps-roles.component';

export let routes: Routes = [
    {
        path: '', 
        component: AdmcAppsComponent,
        children: [
            {
                path: 'roles',
                component: AdmcAppsRolesComponent
            }
        ]
    }
];