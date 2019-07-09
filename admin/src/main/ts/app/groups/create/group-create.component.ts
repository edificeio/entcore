import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { GroupsStore } from '../groups.store';
import { GroupModel } from '../../core/store/models';
import { NotifyService, SpinnerService } from '../../core/services';

import { trim } from '../../shared/utils/string';

import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/toPromise';

@Component({
    selector: 'group-create',
    template: `
        <div class="panel-header">
            <span><s5l>new.group.creation</s5l></span>
        </div>

        <panel-section class="thin">
            <form #createForm="ngForm" (ngSubmit)="createNewGroup()">
                <form-field label="create.group.name">
                    <input type="text" [(ngModel)]="newGroup.name" name="name"
                           required pattern=".*\\S+.*" #nameInput="ngModel"
                           (blur)="onGroupNameBlur(newGroup.name)">
                    <form-errors [control]="nameInput"></form-errors>
                </form-field>

                <div class="action">
                    <button type="button" class="cancel" (click)="cancel()">
                        <s5l>create.group.cancel</s5l>
                    </button>
                    <button class="create confirm"
                            [disabled]="createForm.pristine || createForm.invalid">
                        <s5l>create.group.submit</s5l>
                    </button>
                </div>
            </form>
        </panel-section>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupCreate {

    newGroup: GroupModel = new GroupModel();

    constructor(private http: HttpClient,
                private groupsStore: GroupsStore,
                private ns: NotifyService,
                private spinner: SpinnerService,
                private router: Router,
                private route: ActivatedRoute,
                private location: Location) {
    }

    createNewGroup() {
        this.newGroup.structureId = this.groupsStore.structure.id;

        this.spinner.perform('portal-content', this.http.post<{ id: string }>('/directory/group', {
                name: this.newGroup.name,
                structureId: this.newGroup.structureId
            }).flatMap(groupIdHolder =>
                this.http.post<{ number: number }>(`/communication/group/${groupIdHolder.id}`, {
                    direction: 'BOTH'
                }).map(() => groupIdHolder)
            ).do(groupIdHolder => {
                this.newGroup.id = groupIdHolder.id;
                this.newGroup.type = 'ManualGroup';
                this.groupsStore.structure.groups.data.push(this.newGroup);

                this.ns.success({
                    key: 'notify.group.create.content',
                    parameters: {group: this.newGroup.name}
                }, 'notify.group.create.title');

                this.router.navigate(['..', groupIdHolder.id, 'details'],
                    {relativeTo: this.route, replaceUrl: false});
            }).catch(err => {
                this.ns.error({
                    key: 'notify.group.create.error.content',
                    parameters: {group: this.newGroup.name}
                }, 'notify.group.create.error.title', err);
                throw err;
            }).toPromise()
        )
    }

    cancel() {
        this.location.back();
    }

    onGroupNameBlur(name: string): void {
        this.newGroup.name = trim(name);
    }
}
