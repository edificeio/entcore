import { NgxOdeSijilModule } from 'ngx-ode-sijil';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AlertesRoutingModule } from './alertes-routing.module';
import { AlertesComponent } from './alertes.component';
import { AlerteComponent } from './alerte/alerte.component';
import { AlertesListComponent } from './alertes-list/alertes-list.component';
import {NgxOdeUiModule} from 'ngx-ode-ui';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatListModule } from '@angular/material/list';
import {FlexLayoutModule} from '@angular/flex-layout';
import { AlertesTraiteesListModalComponent} from './alertes-traitees-list-modal/alertes-traitees-list-modal.component';
import { TraitementAlerteModalComponent } from './traitement-alerte-modal/traitement-alerte-modal.component';


@NgModule({
  declarations: [AlertesComponent, AlerteComponent, AlertesListComponent, AlertesTraiteesListModalComponent, TraitementAlerteModalComponent],
    imports: [
        CommonModule,
        NgxOdeSijilModule.forChild(),
        AlertesRoutingModule,
        NgxOdeUiModule,
        MatDialogModule,
        MatListModule,
        MatGridListModule,
        FlexLayoutModule,
        MatCardModule
    ],
    entryComponents: [AlertesTraiteesListModalComponent, TraitementAlerteModalComponent]
})
export class AlertesModule { }
