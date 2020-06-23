import {ChangeDetectionStrategy, Component, Injector, OnInit} from '@angular/core';
import {ZimbraService} from './zimbra.service';
import {routing} from '../../core/services/routing.service';
import {StructureModel} from '../../core/store/models/structure.model';
import {Data} from '@angular/router';
import {OdeComponent} from 'ngx-ode-core';
import {GroupModel} from '../../core/store/models/group.model';
import {NotifyService} from '../../core/services/notify.service';


@Component({
    selector: 'ode-zimbra',
    templateUrl: './zimbra.component.html',
    styleUrls: ['./zimbra.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})

export class ZimbraComponent extends OdeComponent implements OnInit {
    private structure: StructureModel;
    public groups: GroupModel[] = [];
    public roleId: string;
    public checkboxes: boolean[] = [];

    constructor(injector: Injector,
                private zimbraService: ZimbraService,
                private notifyService: NotifyService) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.groups = [];
                this.checkboxes = [];
                this._getGroups();
            }
        }));
    }
    /**
     * Get all groups for the current structure.
     * Sort by boolean (if a checkbox is checked or not) and alphabetically.
     */
    private _getGroups() {
        this.zimbraService.getRoleId().subscribe((data) => {
            this.roleId = data.role.id;
            this.zimbraService.getGroups(this.structure.id).subscribe((groupData) => {
                this.groups = groupData;
                this.groups.sort((a, b) => (+b.roles.includes(this.roleId)) - (+a.roles.includes(this.roleId)) || a.name.localeCompare(b.name));
                this.groups.forEach(
                    (group: GroupModel) => {
                        this.checkboxes.push(group.roles.includes(this.roleId));
                    }
                );

                this.changeDetector.detectChanges();
            });
        });
    }
    /**
     * Remove the permission for the selected group.
     * @param groupModel The group selected.
     */
    private _deletePermission(group: GroupModel) {
        this.zimbraService.deletePermission(group.id, this.roleId).subscribe(
            () => {
                this.notifyService.success(
                    {key: 'management.zimbra.permission.delete.success.content',
                        parameters: {group: group.name }},
                    'management.zimbra.permission.delete.success.title');
            },
            (err) => {
                this.notifyService.notify(
                    'management.zimbra.permission.delete.error.content',
                    'management.zimbra.permission.delete.error.title', err.error.error, 'error');
            }
        );

    }
    /**
     * Grant the permission for the selected group.
     * @param groupModel The group selected.
     */
    private _givePermission(group: GroupModel) {
        this.zimbraService.givePermission(group.id, this.roleId).subscribe(
            () => {
                this.notifyService.success(
                    {key: 'management.zimbra.permission.give.success.content',
                        parameters: {group: group.name }},
                    'management.zimbra.permission.give.success.title');
            },
            (err) => {
                this.notifyService.notify(
                    'management.zimbra.permission.give.error.content',
                    'management.zimbra.permission.give.error.title', err.error.error, 'error');
            }
        );
    }
    /**
     * Update the permission of the selected group when you click in the checkbox.
     * @param index Index of the group selected.
     */
    updatePermission(index: number) {
        const group = this.groups[index];
        if (this.checkboxes[index]) {
            this._givePermission(group);
        } else {
            this._deletePermission(group);
        }
    }
}
