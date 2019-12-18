import {Component, Inject, Injector, OnInit} from '@angular/core';
import {Observable, Subject} from 'rxjs';
import {AlerteModel} from '../../core/store/models/AlerteModel';
import {mergeMap, tap} from 'rxjs/operators';
import {HttpClient} from '@angular/common/http';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';
import {OdeComponent} from 'ngx-ode-core';

@Component({
  selector: 'ode-alertes-traitees-list-modal',
  templateUrl: './alertes-traitees-list-modal.component.html',
  styleUrls: ['./alertes-traitees-list-modal.component.scss']
})
export class AlertesTraiteesListModalComponent extends OdeComponent implements OnInit {

  $doneAlertes = new Subject<AlerteModel[]>();
  $updateSubject: Subject<void> = new Subject<void>();

  constructor(injector: Injector,
              @Inject(MAT_DIALOG_DATA) public data: any,
              private dialogRef: MatDialogRef<AlertesTraiteesListModalComponent>,
              private httpClient: HttpClient) {
    super(injector);
  }

  ngOnInit() {
    this.subscriptions.add(
        this.getDoneAlertes().subscribe(() => {})
    );
    this.subscriptions.add(
        this.$updateSubject
        .pipe(mergeMap(() => this.getDoneAlertes()))
        .subscribe(() => {})
    );
  }

  getDoneAlertes(): Observable<AlerteModel[]> {
    return this.httpClient.get(`timeline/reported?structure=${this.data.structure.id}&page=0&pending=false`)
        .pipe(
            tap( (data: AlerteModel[]) => {
              this.info('getDoneAlertes');
              for (const d of data) {
                d.created.$date = new Date(d.created.$date);
              }
              this.$doneAlertes.next(data);
            })
        );
  }

  close() {
    this.dialogRef.close();
    this.debug('data subject', this.data.subject);
  }
}
