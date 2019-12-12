import { OdeComponent } from 'ngx-ode-core';
import { Component, OnInit, Injector } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {globalStore} from '../../core/store/global.store';
import {routing} from '../../core/services/routing.service';
import {mergeMap, tap} from 'rxjs/operators';
import {Observable, Subject} from 'rxjs';
import {AlerteModel} from '../../core/store/models/AlerteModel';
import {MatDialog} from '@angular/material';
import {AlertesTraiteesListModalComponent} from '../alertes-traitees-list-modal/alertes-traitees-list-modal.component';
import { StructureModel } from 'src/app/core/store/models/structure.model';


@Component({
  selector: 'ode-alertes-list',
  templateUrl: './alertes-list.component.html',
  styleUrls: ['./alertes-list.component.scss']
})
export class AlertesListComponent extends OdeComponent implements OnInit {
    structure: StructureModel;
    pendingAlertes = new Subject<AlerteModel[]>();
    updateSubject = new Subject<void>();

    constructor(injector: Injector,
                private httpClient: HttpClient,
                public dialog: MatDialog) {
      super(injector);
    }

    ngOnInit() {
      super.ngOnInit();
      this.subscriptions.add(
          this.updateSubject
          .pipe(mergeMap( () => this.getPendingAlertes()))
          .subscribe(() => {})
      );
      this.structure = globalStore.structures.data.find(s => s.id === routing.getParam(this.route.snapshot, 'structureId'));
      this.subscriptions.add(
          this.getPendingAlertes().subscribe(() => {})
      );
    }

    getPendingAlertes(): Observable<AlerteModel[]> {
      return this.httpClient.get(`timeline/reported?structure=${this.structure.id}&page=0&pending=true`)
          .pipe(
              tap( (data: AlerteModel[]) => {
                  this.info('getPendingAlertes');
                  this.pendingAlertes.next(data);
              })
          );
    }

    openDialog() {
        const dialogRef = this.dialog.open(AlertesTraiteesListModalComponent, {
            width: '80%',
            data: { structure: this.structure },
            disableClose: true
        });

        this.subscriptions.add(dialogRef.afterClosed().subscribe(result => {
            this.subscriptions.add(
                this.getPendingAlertes().subscribe(() => {})
            );
        }));
    }

}
