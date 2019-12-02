import { OdeComponent } from './../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';
import {Location} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {HttpClient} from '@angular/common/http';

import {GroupsStore} from '../../groups.store';
import {GroupModel} from '../../../core/store/models/group.model';

import {trim} from '../../../shared/utils/string';
import {catchError, flatMap, map, tap} from 'rxjs/operators';
import { NotifyService } from 'src/app/core/services/notify.service';
import { SpinnerService } from 'src/app/core/services/spinner.service';



@Component({
    selector: 'ode-group-create',
    templateUrl: './group-create.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupCreateComponent extends OdeComponent{

    newGroup: GroupModel = new GroupModel();

    constructor(private http: HttpClient,
                private groupsStore: GroupsStore,
                private ns: NotifyService,
                private spinner: SpinnerService,
                injector: Injector,
                private location: Location) {
                  super(injector);
    }

    createNewGroup() {
        this.newGroup.structureId = this.groupsStore.structure.id;

        this.spinner.perform('portal-content', this.http.post<{ id: string }>('/directory/group', {
                name: this.newGroup.name,
                structureId: this.newGroup.structureId
            }).pipe(
          flatMap(groupIdHolder =>
            this.http.post<{ number: number }>(`/communication/group/${groupIdHolder.id}`, {
              direction: 'BOTH'
            }).pipe(map(() => groupIdHolder))
          ),
          tap(groupIdHolder => {
            this.newGroup.id = groupIdHolder.id;
            this.newGroup.type = 'ManualGroup';
            this.groupsStore.structure.groups.data.push(this.newGroup);

            this.ns.success({
              key: 'notify.group.create.content',
              parameters: {group: this.newGroup.name}
            }, 'notify.group.create.title');

            this.router.navigate(['..', groupIdHolder.id, 'details'],
              {relativeTo: this.route, replaceUrl: false});
          }),
          catchError( err => {
            this.ns.error({
              key: 'notify.group.create.error.content',
              parameters: {group: this.newGroup.name}
            }, 'notify.group.create.error.title', err);
            throw err;
          })
        ).toPromise()
        );
    }

    cancel() {
        this.location.back();
    }

    onGroupNameBlur(name: string): void {
        this.newGroup.name = trim(name);
    }
}
