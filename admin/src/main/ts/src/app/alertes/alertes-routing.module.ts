import { AlertesComponent } from './alertes.component';
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AlertesListComponent } from './alertes-list/alertes-list.component';


const routes: Routes = [
  {
    path: '', component: AlertesComponent,
    children: [
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
