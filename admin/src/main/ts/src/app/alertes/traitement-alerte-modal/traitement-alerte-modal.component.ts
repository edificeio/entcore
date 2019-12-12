import {Component, Inject, Injector, OnInit} from '@angular/core';
import {OdeComponent} from 'ngx-ode-core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'ode-traitement-alerte-modal',
  templateUrl: './traitement-alerte-modal.component.html',
  styleUrls: ['./traitement-alerte-modal.component.scss']
})
export class TraitementAlerteModalComponent extends OdeComponent implements OnInit {

  constructor(injector: Injector,
              private httpClient: HttpClient,
              @Inject(MAT_DIALOG_DATA) public data: any,
              private dialogRef: MatDialogRef<TraitementAlerteModalComponent>) {
    super(injector);
  }

  close() {
    if (this.data && this.data.subject) {
      this.data.subject.next();
    }
    this.dialogRef.close();
  }

  ngOnInit() {
    super.ngOnInit();
  }


  conserver() {
    this.subscriptions.add(
    this.httpClient.put(`timeline/${this.data.alertId}/action/keep?structure=${this.data.structureId}`, { structure: this.data.structureId})
        .subscribe(() => {
          this.close();
        }));
  }

  supprimer() {
    this.subscriptions.add(
        this.httpClient.put(`timeline/${this.data.alertId}/action/delete?structure=${this.data.structureId}`, { structure: this.data.structureId})
        .subscribe(() => {
          this.close();
        })
    );
  }

}
