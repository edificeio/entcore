import { AlertesComponent } from './alertes.component';
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AlertesListComponent } from './alertes-list/alertes-list.component';
import { ReportedContentsComponent } from './reported-contents/reported-contents.component';
import { AlertesDefaultTabGuardService, ReportedContentsGuardService } from './reported-contents/reported-contents.guards';


const routes: Routes = [
  {
    path: '', component: AlertesComponent,
    children: [
      {
          // Redirects to the default tab, depending on the user's rights
          path: '', pathMatch: 'full', canActivate: [AlertesDefaultTabGuardService], children: []
      },
      {
          path: 'contenus', component: ReportedContentsComponent, canActivate: [ReportedContentsGuardService]
      },
      {
          path: 'signalements', component: AlertesListComponent
      }]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AlertesRoutingModule { }
