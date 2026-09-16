import { OdeComponent } from 'ngx-ode-core';
import {Component, OnInit, Injector, Input, Output} from '@angular/core';
import {AlerteModel} from '../../core/store/models/AlerteModel';
import {ReplaySubject, Subject} from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
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
    const reportDate = this.parseDateWithShortOffset(value.reporters[0]?.date);
    this._firstReportDate = reportDate
      ? reportDate.toLocaleString("fr-FR")
      : new Date(value.created.$date).toLocaleString("fr-FR");
    // Remove unused seconds
    if (this._firstReportDate && this._firstReportDate.length > 3)
      this._firstReportDate = this._firstReportDate.substring(
        0,
        this._firstReportDate.length - 3,
      );
    this.buildReportersString();
  }

  /**
   * Parse une date au format "YYYY-MM-DDTHH:mm±HH" (offset sans les minutes,
   * non conforme ISO 8601 strict) et renvoie un objet Date valide, ou null.
   */
  private parseDateWithShortOffset(raw?: string): Date | null {
    if (!raw) {
      return null;
    }
    const match = raw.match(/^(.*T\d{2}:\d{2}(?::\d{2})?)([+-]\d{1,2})$/);
    const normalized = match
      ? `${match[1]}${match[2][0]}${match[2].slice(1).padStart(2, "0")}:00`
      : raw;
    const parsed = new Date(normalized);
    return isNaN(parsed.getTime()) ? null : parsed;
  }
  get firstReportDate(): string {
    return this._firstReportDate;
  }

  reporters: ReplaySubject<string> = new ReplaySubject<string>();
  private _alerte: AlerteModel;
  private _firstReportDate: string;


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
