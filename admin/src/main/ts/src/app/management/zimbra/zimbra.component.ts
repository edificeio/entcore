import {ChangeDetectionStrategy, Component, Injector, OnInit} from '@angular/core';
import {ZimbraService} from './zimbra.service';
import {routing} from '../../core/services/routing.service';
import {StructureModel} from '../../core/store/models/structure.model';
import {Data} from '@angular/router';
import {OdeComponent} from 'ngx-ode-core';
import {GroupModel} from '../../core/store/models/group.model';
import {NotifyService} from '../../core/services/notify.service';
import { RecallMail } from './recallmail.model';
import {switchMap} from 'rxjs/operators';
import {BundlesService} from 'ngx-ode-sijil';
import { ActionStatus } from 'src/app/management/zimbra/enum/action-status.enum';

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
    public checkboxesMail: boolean[] = [];
    public recalledMails: RecallMail[] = [];
    public removeConfirmationDisplayed = false;
    public deleteConfirmationDisplayed = false;
    public detailLightboxDisplayed = false;
    public recallMail: RecallMail;

    constructor(injector: Injector,
                private zimbraService: ZimbraService,
                private notifyService: NotifyService,
                private bundles: BundlesService) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        /* Get the current structure and init arrays. */
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.groups = [];
                this.checkboxes = [];
                this.checkboxesMail = [];
                this._getGroups();
                this._getRecalledMails();
            }
        }));
    }

    refreshMails(): void {
        this._getRecalledMails();
    }

    /**
     * Get all groups for the current structure.
     * Get the role id corresponding to Zimbra.
     * Sort by boolean (if a checkbox is checked or not) and alphabetically.
     */
    private _getGroups(): void {
        this.subscriptions.add(this.zimbraService.getRoleId().pipe(
                switchMap(data => {
                    this.roleId = data.role.id;
                    return this.zimbraService.getGroups(this.structure.id);
                })
            ).subscribe(groupData => {
                this.groups = groupData;
                this.groups.sort((a, b) => (+b.roles.includes(this.roleId)) - (+a.roles.includes(this.roleId)) || a.name.localeCompare(b.name));
                this.groups.forEach(
                    (group: GroupModel) => {
                        this.checkboxes.push(group.roles.includes(this.roleId));
                    }
                );
                this.changeDetector.detectChanges();
            })
        );
    }

    /**
     * Remove the permission for the selected group.
     * @param groupModel The selected group.
     */
    private _deletePermission(group: GroupModel): void {
        this.subscriptions.add(this.zimbraService.deletePermission(group.id, this.roleId).subscribe(
            () => {
                this.notifyService.success(
                    {
                        key: 'management.zimbra.permission.delete.success.content',
                        parameters: {group: group.name}
                    },
                    'management.zimbra.permission.delete.success.title');
            },
            (err) => {
                this.notifyService.notify(
                    'management.zimbra.permission.delete.error.content',
                    'management.zimbra.permission.delete.error.title', err.error.error, 'error');
            }
        ));

    }

    /**
     * Grant the permission for the selected group.
     * @param groupModel The selected group.
     */
    private _givePermission(group: GroupModel): void {
        this.subscriptions.add(this.zimbraService.givePermission(group.id, this.roleId).subscribe(
            () => {
                this.notifyService.success(
                    {
                        key: 'management.zimbra.permission.give.success.content',
                        parameters: {group: group.name}
                    },
                    'management.zimbra.permission.give.success.title');
            },
            (err) => {
                this.notifyService.notify(
                    'management.zimbra.permission.give.error.content',
                    'management.zimbra.permission.give.error.title', err.error.error, 'error');
            }
        ));
    }

    /**
     * Update the permission of the selected group when you click on the checkbox.
     * @param index Index of the selected group.
     */
    public updatePermission(index: number): void {
        const group: GroupModel = this.groups[index];
        if (this.checkboxes[index]) {
            this._givePermission(group);
        } else {
            this._deletePermission(group);
        }
    }

    /**
     * Get the list of recalled mails to validate.
     */
    private _getRecalledMails(): void {
        this.subscriptions.add(this.zimbraService.getRecalledMails(this.structure.id).subscribe(
            (recalledMails) => {
                this.recalledMails = Array.from(recalledMails).sort((a, b) => b.action.date - a.action.date);
                this.recalledMails.forEach(mail => {
                    this.checkboxesMail[mail.recallMailId] = false;
                    let status = RecallMail.getActionStatus(mail.action);
                    mail.statutDisplayed = this.bundles.translate('management.zimbra.return.statut.' + status);
                    mail.status = status;
                });
                this.changeDetector.detectChanges();
            },
            (err) => {
                this.notifyService.notify(
                    'management.zimbra.return.delete.error.content',
                    'management.zimbra.return.delete.error.title', err.error.error, 'error');
            }
        ));
    }

    public deleteRecalledMail(recallMail: RecallMail): void {
        this.subscriptions.add(this.zimbraService.deleteRecalledMail(recallMail.recallMailId).subscribe(() => {
                this.refreshMails();
                this.notifyService.success(
                    'management.zimbra.return.delete.success.content',
                    'management.zimbra.return.delete.success.title');
                this.changeDetector.detectChanges();
            },
            (err) => {
                this.notifyService.notify(
                    'management.zimbra.return.delete.error.content',
                    'management.zimbra.return.delete.error.title', err.error.error, 'error');
            }));
    }



    public removeSelectedRecallMails(): void {
        const ids: number[] = [];
        this.recalledMails.filter(message => this.checkboxesMail[message.recallMailId]).forEach(message => {
            ids.push(message.recallMailId);
        });
        this.subscriptions.add(this.zimbraService.acceptRecalls(ids).subscribe(() => {
            this.refreshMails();
        },
        (err) => {
            this.notifyService.notify(
                'management.zimbra.return.remove.error.content',
                'management.zimbra.return.remove.error.title', err.error.error, 'error');
        }));
    }

    public getSelectedRecalledMail(): RecallMail[] {
        return this.recalledMails.filter(message => this.checkboxesMail[message.recallMailId])
    }

    /**
     * Check checkbox if statut is WAITING or ERROR.
     */
    public checkCheckBox(recallMail: RecallMail) {
        if (recallMail.status === 'WAITING' || recallMail.status === 'ERROR') {
            this.checkboxesMail[recallMail.recallMailId] = !this.checkboxesMail[recallMail.recallMailId];
        }
    }

    /**
     * Check if all recalled mails are checked (excepted removed and progress).
     */
    public areAllChecked(): boolean {
        return this.recalledMails.filter(mess => mess.status === 'WAITING' || mess.status === 'ERROR').length > 0 &&
            this.recalledMails.every(mess => mess.status === 'REMOVED' || mess.status === 'PROGRESS' || this.checkboxesMail[mess.recallMailId]);
    }

    /**
     * Check all recalled mails (excepted removed and progress).
     */
    public checkAll(): void {
        const allChecked = this.areAllChecked();
        this.recalledMails.forEach(mess => {
            if (mess.status !== 'REMOVED' && mess.status !== 'PROGRESS') {
                this.checkboxesMail[mess.recallMailId] = !allChecked;
            }
        });
    }

    getProgressionMessageDependingOnRecalls(recallMail: RecallMail) {
        if (recallMail.status == ActionStatus.WAITING) {
            return recallMail.action.tasks.total.toString() + " " + this.bundles.translate('management.zimbra.recall.number.message' + (recallMail.action.tasks.total > 1 ? ".plural" : ""));
        } else {
            return recallMail.action.tasks.finished.toString() + " / " + recallMail.action.tasks.total.toString();
        }
    }

    /**
     * Open lightbox which give details of a recalled mail.
     */
    public openDetailLightBox(recallMail: RecallMail): void {
        this.recallMail = recallMail;
        this.detailLightboxDisplayed = true;
    }

    /**
     * Open lightbox which delete a recalled mail.
     */
    public openDeleteLightBox(recalledMail: RecallMail): void {
        this.recallMail = recalledMail;
        this.deleteConfirmationDisplayed = true;
    }

    /**
     * Open lightbox which recalled mail.
     */
    public openPopUpRemoveConfirmation(): void {
        this.removeConfirmationDisplayed = true;
    }

    /**
     * Convert second to hh:mm:ss.
     */
    private _secondsToHms(d: number): string {
        const h = Math.floor(d / 3600);
        const m = Math.floor(d % 3600 / 60);
        const s = Math.floor(d % 3600 % 60);

        const hDisplay = h > 10 ? h.toString() : '0' + h;
        const mDisplay = m > 10 ? m.toString() : '0' + m;
        const sDisplay = s > 10 ? s.toString() : '0' + s;
        return hDisplay + ':' + mDisplay + ':' + sDisplay;
    }
}
