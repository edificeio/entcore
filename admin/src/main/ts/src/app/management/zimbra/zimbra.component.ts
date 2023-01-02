import {ChangeDetectionStrategy, Component, Injector, OnInit} from '@angular/core';
import {ZimbraService} from './zimbra.service';
import {routing} from '../../core/services/routing.service';
import {StructureModel} from '../../core/store/models/structure.model';
import {Data} from '@angular/router';
import {OdeComponent} from 'ngx-ode-core';
import {GroupModel} from '../../core/store/models/group.model';
import {NotifyService} from '../../core/services/notify.service';
import {ReturnedMail, ReturnedMailStatut} from './ReturnedMail';
import {switchMap} from 'rxjs/operators';
import {BundlesService} from 'ngx-ode-sijil';

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
    public returnedMails: ReturnedMail[] = [];
    public removeConfirmationDisplayed = false;
    public deleteConfirmationDisplayed = false;
    public detailLightboxDisplayed = false;
    public returnedMail = new ReturnedMail();

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
                this._getReturnedMails();
            }
        }));
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
     * Get the list of returned mails to validate.
     */
    private _getReturnedMails(): void {
        this.subscriptions.add(this.zimbraService.getReturnedMails(this.structure.id).subscribe(
            (returnedMails) => {
                this.returnedMails = returnedMails;
                this.returnedMails.forEach(mail => {
                    this.checkboxesMail[mail.id] = false;
                    mail.statutDisplayed = this.bundles.translate('management.zimbra.return.statut.' + mail.statut);
                    mail.estimatedTime = this._secondsToHms(mail.number_message);
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

    /**
     * Delete a returned mail from table
     */
    public deleteReturnedMail(returnedMail: ReturnedMail): void {
        this.subscriptions.add(this.zimbraService.deleteReturnedMail(returnedMail.id).subscribe(
            (returnedMailDelete) => {
                this.returnedMails.splice(this.returnedMails.findIndex(mail => mail.id === returnedMailDelete.id), 1);
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

    /**
     * Remove all selected returned mails.
     */
    public removeSelectedReturnedMails(): void {
        const ids: number[] = [];
        this.getSelectedReturnedMail().forEach(mail => {
            ids.push(mail.id);
            mail.statutDisplayed = this.bundles.translate('management.zimbra.return.statut.PROGRESS');
            mail.statut = 'PROGRESS';
            mail.date = new Date().toString();
        });
        this.changeDetector.detectChanges();
        this.subscriptions.add(this.zimbraService.removeReturnedMails(ids).subscribe(
            (mailsResponse: ReturnedMailStatut[]) => {
                let successCount, failCount = 0;
                this.getSelectedReturnedMail().forEach((mail: ReturnedMail) => {
                    mailsResponse.forEach((mailResponse: ReturnedMailStatut) => {
                        if (mailResponse.id === mail.id) {
                            mail.statutDisplayed = this.bundles.translate('management.zimbra.return.statut.' + mailResponse.statut);
                            mail.statut = mailResponse.statut;
                            mail.date = mailResponse.date;
                        }
                        if (mailResponse.statut === 'ERROR') {
                            failCount++;
                        }
                        if (mailResponse.statut === 'REMOVED') {
                            successCount++;
                        }
                    });
                    this.checkboxesMail[mail.id] = false;
                });
                this.changeDetector.detectChanges();
                if (successCount === 0) {
                    this.notifyService.error(
                        'management.zimbra.return.remove.error.content',
                        'management.zimbra.return.remove.error.title');
                } else if (failCount > 0) {
                    this.notifyService.error(
                        { key: 'management.zimbra.return.remove.someFail.content', parameters: { failCount, successCount } },
                        'management.zimbra.return.remove.someFail.title');
                } else {
                    this.notifyService.success(
                        'management.zimbra.return.remove.success.content',
                        'management.zimbra.return.remove.success.title');
                }
            },
            (err) => {
                this.notifyService.notify(
                    'management.zimbra.return.remove.error.content',
                    'management.zimbra.return.remove.error.title', err.error.error, 'error');
            }
        ));
    }

    /**
     * Get all selected returned mails.
     */
    public getSelectedReturnedMail(): ReturnedMail[] {
        const mails = [];
        this.returnedMails.forEach(mess => {
            const isChecked = this.checkboxesMail[mess.id];
            if (isChecked === true) {
                mails.push(mess);
            }
        });
        return mails;
    }

    /**
     * Check checkbox if statut is WAITING or ERROR.
     */
    public checkCheckBox(returnedMail: ReturnedMail) {
        if (returnedMail.statut === 'WAITING' || returnedMail.statut === 'ERROR') {
            this.checkboxesMail[returnedMail.id] = !this.checkboxesMail[returnedMail.id];
        }
    }

    /**
     * Check if all returned mails are checked (excepted removed and progress).
     */
    public areAllChecked(): boolean {
        return this.returnedMails.filter(mess => mess.statut === 'WAITING' || mess.statut === 'PROGRESS').length > 0 &&
            this.returnedMails.every(mess => mess.statut === 'REMOVED' || mess.statut === 'PROGRESS' || this.checkboxesMail[mess.id]);
    }

    /**
     * Check all returned mails (excepted removed and progress).
     */
    public checkAll(): void {
        const allChecked = this.areAllChecked();
        this.returnedMails.forEach(mess => {
            if (mess.statut !== 'REMOVED' && mess.statut !== 'PROGRESS') {
                this.checkboxesMail[mess.id] = !allChecked;
            }
        });
    }

    /**
     * Open lightbox which give details of a returned mail.
     */
    public openDetailLightBox(returnedMail: ReturnedMail): void {
        this.returnedMail = returnedMail;
        this.detailLightboxDisplayed = true;
    }

    /**
     * Open lightbox which delete a returned mail.
     */
    public openDeleteLightBox(returnedMail: ReturnedMail): void {
        this.returnedMail = returnedMail;
        this.deleteConfirmationDisplayed = true;
    }

    /**
     * Open lightbox which returned mail.
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
