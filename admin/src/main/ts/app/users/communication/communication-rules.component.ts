import { ChangeDetectorRef, Component, Input } from '@angular/core';
import { GroupModel } from '../../core/store/models';
import { CommunicationRulesService } from './communication-rules.service';
import { GroupNameService, NotifyService } from '../../core/services';
import { Subject } from 'rxjs/Subject';
import { BundlesService } from 'sijil';
import 'rxjs/add/operator/first';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/mergeMap';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

const css = {
    group: 'lct-user-communication-group',
    sendingColumn: 'lct-user-communication-sending-column',
    receivingColumn: 'lct-user-communication-receiving-column'
};

export const communicationRulesLocators = {
    group: `.${css.group}`,
    sendingColumn: `.${css.sendingColumn}`,
    receivingColumn: `.${css.receivingColumn}`
};

const WARNING_ENDGROUP_USERS_CAN_COMMUNICATE = "endgroup-users-can-communicate";
const WARNING_STARTGROUP_USERS_CAN_COMMUNICATE = "startgroup-users-can-communicate";
const WARNING_BOTH_GROUPS_USERS_CAN_COMMUNICATE = "both-groups-users-can-communicate";

@Component({
    selector: 'communication-rules',
    template: `
        <div class="communication-rules__headers">
                <span class="communication-rules__header communication-rules__header--sending">{{ sendingHeaderLabel }}</span>
                <span class="communication-rules__header communication-rules__header--receiving">{{ receivingHeaderLabel }}</span>
        </div>
        <div class="communication-rules__columns">
            <div class="communication-rules__column communication-rules__column--sending ${css.sendingColumn}">
                <div class="group ${css.group}" *ngFor="let group of getSenders(); trackBy: trackByGroupId">
                    <group-card
                    (click)="activeColumn === 'sending' ? select('sending', group) : $event.stopPropagation()"
                    (clickOnRemoveCommunication)="removeCommunication(group, selected.group)"
                    (mouseenter)="highlight('sending', group, selected)"
                    (mouseleave)="resetHighlight()"
                    [group]="group"
                    [selectable]="activeColumn === 'sending'"
                    [selected]="isSelected('sending', group, selected)"
                    [highlighted]="isRelatedWithCell('sending', group, highlighted, communicationRules)"
                    [active]="isRelatedWithCell('sending', group, selected, communicationRules)"
                    (clickAddCommunication)="openGroupPicker($event)"></group-card>
                </div>
            </div>
            <div class="communication-rules__column communication-rules__column--receiving ${css.receivingColumn}">
                <div class="group ${css.group}" *ngFor="let group of getReceivers(); trackBy: trackByGroupId">
                    <group-card
                    (click)="activeColumn === 'receiving' ? select('receiving', group) : $event.stopPropagation()"
                    (clickOnRemoveCommunication)="removeCommunication(selected.group, group)"
                    (mouseenter)="highlight('receiving', group, selected)"
                    (mouseleave)="resetHighlight()"
                    [group]="group"
                    [selectable]="activeColumn === 'receiving'"
                    [selected]="isSelected('receiving', group, selected)"
                    [highlighted]="isRelatedWithCell('receiving', group, highlighted, communicationRules)"
                    [active]="isRelatedWithCell('receiving', group, selected, communicationRules)"
                    (clickAddCommunication)="openGroupPicker($event)"></group-card>
                </div>
            </div>
        </div>
        <lightbox-confirm *ngIf="!!selected" lightboxTitle="user.communication.action.confirm.title"
                          [show]="removeConfirmationDisplayed"
                          (onCancel)="removeConfirmationClicked.next('cancel')"
                          (onConfirm)="removeConfirmationClicked.next('confirm')">
            <i class='fa fa-exclamation-triangle is-danger'></i> 
            <span [innerHTML]="'user.communication.remove-communication.confirm.content' | translate: {groupName: groupNameService.getGroupName(selected.group)}"></span>
        </lightbox-confirm>
        
        <group-picker lightboxTitle="services.roles.groups.add"
            [list]="addCommunicationPickableGroups"
            [filters]="filterGroupPicker"
            [types]="['ProfileGroup', 'FunctionalGroup', 'ManualGroup']"
            [show]="showGroupPicker"
            sort="name"
            searchPlaceholder="search.group"
            noResultsLabel="list.results.no.groups"
            (pick)="onGroupPick($event)"
            (close)="showGroupPicker = false;">
        </group-picker>
        
        <lightbox-confirm *ngIf="!!selected" lightboxTitle="user.communication.action.confirm.title"
                          [show]="addConfirmationDisplayed"
                          (onCancel)="addConfirmationClicked.next('cancel')"
                          (onConfirm)="addConfirmationClicked.next('confirm')">
            <div>
                <i class='fa fa-exclamation-triangle is-danger'></i> 
                <span [innerHTML]="'user.communication.add-communication.confirm.content' | translate: {groupName: groupNameService.getGroupName(selected.group)}"></span>
            </div>
            <div *ngIf="warningGroupSender" class="has-top-margin-10">
                <i class='fa fa-exclamation-triangle is-danger'></i> 
                <span [innerHTML]="'user.communication.add-communication.confirm.users-can-communicate' | translate: {groupName: groupNameService.getGroupName(selected.group)}"></span>
            </div>
            <div *ngIf="warningGroupReceiver" class="has-top-margin-10">
                <i class='fa fa-exclamation-triangle is-danger'></i> 
                <span [innerHTML]="'user.communication.add-communication.confirm.users-can-communicate' | translate: {groupName: groupNameService.getGroupName(pickedGroup)}"></span>
            </div>
        </lightbox-confirm>`,
    styles: [`
        .communication-rules__header {
            font-size: 20px;
        }
    `, `
        .communication-rules__headers, .communication-rules__columns {
            display: flex;
        }
    `, `
        .communication-rules__header, .communication-rules__column {
            flex-grow: 1;
            flex-basis: 0;
        }
    `, `
        group-card {
            display: inline-block;
        }
    `, `
        .communication-rules__column.communication-rules__column--sending,
        .communication-rules__header.communication-rules__header--sending {
            margin-right: 10px;
            margin-bottom: 10px;
        }
    `, `
        .communication-rules__column.communication-rules__column--receiving,
        .communication-rules__header.communication-rules__header--receiving {
            margin-left: 10px;
            margin-bottom: 10px;
        }
    `]
})
export class CommunicationRulesComponent {
    @Input()
    public sendingHeaderLabel: string = '';

    @Input()
    public receivingHeaderLabel: string = '';

    @Input()
    public communicationRules: CommunicationRule[];

    @Input()
    public addCommunicationPickableGroups: GroupModel[];

    @Input()
    public activeColumn: Column;

    public selected: Cell;
    public highlighted: Cell;

    public removeConfirmationDisplayed = false;
    public removeConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();
    public addConfirmationDisplayed = false;
    public addConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public showGroupPicker: boolean = false;
    public warningGroupSender: boolean = false;
    public warningGroupReceiver: boolean = false;
    public pickedGroup: GroupModel;

    constructor(private communicationRulesService: CommunicationRulesService,
                private notifyService: NotifyService,
                public groupNameService: GroupNameService,
                private bundlesService: BundlesService,
                private changeDetectorRef: ChangeDetectorRef) {
    }

    public select(column: Column, group: GroupModel): void {
        this.selected = {column, group};
        this.resetHighlight();
    }

    public highlight(column: Column, group: GroupModel, selected: Cell): void {
        if (!selected || column !== selected.column || group.id !== selected.group.id) {
            this.highlighted = {column, group};
        }
    }

    public resetHighlight(): void {
        this.highlighted = null;
    }

    public getSenders(): GroupModel[] {
        const senders = this.communicationRules
            .map(rule => rule.sender)
            .filter(group => !!group);
        return uniqueGroups(senders);
    }

    public getReceivers(): GroupModel[] {
        const receivers: GroupModel[] = [];
        this.communicationRules.forEach(rule => receivers.push(...rule.receivers));
        return uniqueGroups(receivers);
    }

    public trackByGroupId(index: number, group: GroupModel): string {
        return group.id;
    }

    public isSelected(column: Column, group: GroupModel, selected: Cell) {
        if (!selected) {
            return false;
        }

        return group.id === selected.group.id && column === selected.column;
    }

    public isRelatedWithCell(column: Column, group: GroupModel, cell: Cell, communicationRules: CommunicationRule[]): boolean {
        if (!cell) {
            return false;
        }

        if (column === cell.column) {
            return group.id === cell.group.id;
        }

        return communicationRules
            .filter(cr => cell.column === 'sending' ? (cr.sender ? (cr.sender.id === cell.group.id) : false) : cr.receivers.some(g => g.id === cell.group.id))
            .some(cr => cell.column === 'sending' ? cr.receivers.some(g => g.id === group.id) : (cr.sender ? (cr.sender.id === group.id) : false));
    }

    public removeCommunication(sender: GroupModel, receiver: GroupModel) {
        this.removeConfirmationDisplayed = true;
        this.removeConfirmationClicked.asObservable()
            .first()
            .do(() => this.removeConfirmationDisplayed = false)
            .filter(choice => choice === 'confirm')
            .mergeMap(() => this.communicationRulesService.removeCommunication(sender, receiver, this.activeColumn))
            .subscribe(() => this.notifyService.success({
                key: 'user.communication.remove-communication.success',
                parameters: {groupName: this.groupNameService.getGroupName(sender)}
            }), () => this.notifyService.error({
                key: 'user.communication.remove-communication.error.content',
                parameters: {groupName: this.groupNameService.getGroupName(sender)}
            }, 'user.communication.remove-communication.error.title'));
    }

    public openGroupPicker(): void {
        this.showGroupPicker = true;
    }

    public filterGroupPicker = (group: GroupModel) => {
        if (this.selected) {
            if (group.id === this.selected.group.id) {
                return false;
            }

            if (this.activeColumn === 'sending') {
                return !this.communicationRules
                    .find(commRule => commRule.sender.id == this.selected.group.id).receivers
                    .find(receiver => receiver.id === group.id);
            } else {
                return !this.communicationRules
                    .filter(commRule => commRule.receivers.some(receiver => receiver.id === this.selected.group.id))
                    .some(commRule => !!commRule.sender && (commRule.sender.id == group.id));
            }
        }
        return true;
    };

    public onGroupPick(pickedGroup: GroupModel) {
        const sender: GroupModel = this.activeColumn === 'sending' ? this.selected.group : pickedGroup;
        const receiver: GroupModel = this.activeColumn === 'sending' ? pickedGroup : this.selected.group;
        this.communicationRulesService.checkAddLink(sender, receiver)
            .catch((error: HttpErrorResponse) => {
                if (error.status === 409) {
                    this.notifyService.error('user.communication.add-communication.error.impossible.content'
                        , 'user.communication.add-communication.error.impossible.title')
                } else {
                    this.notifyService.error({
                        key: 'group.internal-communication-rule.change.error.content',
                        parameters: {groupName: this.groupNameService.getGroupName(this.selected.group)}
                    }, 'group.internal-communication-rule.change.error.title')
                }
                return Observable.throw({printed: true, original: error});
            })
            .do(res => {
                if (res.warning === WARNING_STARTGROUP_USERS_CAN_COMMUNICATE) {
                    this.warningGroupSender = true;
                } else if (res.warning === WARNING_ENDGROUP_USERS_CAN_COMMUNICATE) {
                    this.warningGroupReceiver = true;
                } else if (res.warning === WARNING_BOTH_GROUPS_USERS_CAN_COMMUNICATE) {
                    this.warningGroupSender = true;
                    this.warningGroupReceiver = true;
                }
                this.pickedGroup = pickedGroup;
                this.addConfirmationDisplayed = true;
                this.changeDetectorRef.markForCheck();
            })
            .mergeMap(() => this.addConfirmationClicked.asObservable())
            .first()
            .do(() => this.addConfirmationDisplayed = false)
            .filter(choice => choice === 'confirm')
            .mergeMap(() => this.communicationRulesService.createCommunication(sender, receiver, this.activeColumn))
            .subscribe(() => this.notifyService.success('user.communication.add-communication.success'),
                (err) => {
                    if (!err.printed) {
                        this.notifyService.error({
                            key: 'user.communication.add-communication.error.content',
                            parameters: {groupName: this.selected.group.name}
                        }, 'user.communication.add-communication.error.title');
                    }
                });
    }
}

export interface CommunicationRule {
    sender: GroupModel,
    receivers: GroupModel[]
}

export function uniqueGroups(groups: GroupModel[]): GroupModel[] {
    const uniqGroups: GroupModel[] = [];
    groups.forEach(group => {
        if (!uniqGroups.some(g => g.id === group.id)) {
            uniqGroups.push(group);
        }
    });
    return uniqGroups;
}

export type Column = 'sending' | 'receiving';

interface Cell {
    column: Column;
    group: GroupModel;
}
