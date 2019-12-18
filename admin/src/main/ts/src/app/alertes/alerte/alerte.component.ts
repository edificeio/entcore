import { OdeComponent } from 'ngx-ode-core';
import {Component, OnInit, Injector, Input, Output} from '@angular/core';
import {AlerteModel} from '../../core/store/models/AlerteModel';
import {ReplaySubject, Subject} from 'rxjs';
import {MatDialog} from '@angular/material';
import {TraitementAlerteModalComponent} from '../traitement-alerte-modal/traitement-alerte-modal.component';

@Component({
  selector: 'ode-alerte',
  templateUrl: './alerte.component.html',
  styleUrls: ['./alerte.component.scss']
})
export class AlerteComponent extends OdeComponent implements OnInit {

  @Input() structure: any;
  @Input() subject: Subject<void>;
  get alerte(): AlerteModel {
    return this._alerte;
  }
  @Input()
  set alerte(value: AlerteModel) {
    this._alerte = value;
    console.log('set alerte', value)
    this._alerte.created.$date = new Date(this._alerte.created.$date).toLocaleString('fr-FR');
    this.buildReportersString();
  }

  reporters: ReplaySubject<string> = new ReplaySubject<string>();
  private _alerte: AlerteModel;


  constructor(injector: Injector,
              public dialog: MatDialog) {
    super(injector);
  }

  buildReportersString() {
    let reportersString = '';
    for ( const reporter of this.alerte.reporters) {
      reportersString += `${reporter.firstName} ${reporter.lastName}`;
    }
    this.reporters.next (reportersString);
  }

  ngOnInit() {
    super.ngOnInit();

  }

  onSignalementClic() {
    const dialogRef = this.dialog.open(TraitementAlerteModalComponent, {
      width: '80%',
      data: { structureId: this.structure.id,
              alertId: this.alerte._id,
              subject: this.subject},
              disableClose: true
      });

    this.subscriptions.add(
      dialogRef.afterClosed().subscribe(result => {
        this.info('The dialog was closed Alerte');
      })
    );
  }
}
