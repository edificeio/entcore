import { Component, Input, ChangeDetectorRef } from '@angular/core';
import { GroupModel, SessionModel } from '../../core/store/models';
import { CommunicationRulesService } from './communication-rules.service';
import { NotifyService } from '../../core/services';
import { GroupNameService } from './group-name.service';
import { Subject } from 'rxjs/Subject';
import { BundlesService } from 'sijil';
import 'rxjs/add/operator/first';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/mergeMap';
import { HttpErrorResponse } from '@angular/common/http';
import { Session } from '../../core/store';

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
                <span class="communication-rules__header communication-rules__header--sending">{{ 'user.communication.groups-of-user' | translate }}</span>
                <span class="communication-rules__header communication-rules__header--receiving">{{ 'user.communication.groups-that-user-can-communicate-with' | translate }}</span>
        </div>
        <div class="communication-rules__columns">
            <div class="communication-rules__column communication-rules__column--sending ${css.sendingColumn}">
                <div class="group ${css.group}" *ngFor="let group of getSenders(); trackBy: trackByGroupId">
                    <group-card
                    (click)="select('sending', group)"
                    (clickOnRemoveCommunication)="removeCommunication(group, selected.group)"
                    (mouseenter)="highlight('sending', group, selected)"
                    (mouseleave)="resetHighlight()"
                    [group]="group"
                    [selected]="isSelected('sending', group, selected)"
                    [highlighted]="isRelatedWithCell('sending', group, highlighted, communicationRules)"
                    [active]="isRelatedWithCell('sending', group, selected, communicationRules)"
                    (clickAddCommunication)="openGroupPicker($event)"></group-card>
                </div>
            </div>
            <div class="communication-rules__column communication-rules__column--receiving ${css.receivingColumn}">
                <div class="group ${css.group}" *ngFor="let group of getReceivers(); trackBy: trackByGroupId">
                    <group-card
                    (clickOnRemoveCommunication)="removeCommunication(selected.group, group)"
                    (mouseenter)="highlight('receiving', group, selected)"
                    (mouseleave)="resetHighlight()"
                    [group]="group"
                    [selected]="isSelected('receiving', group, selected)"
                    [highlighted]="isRelatedWithCell('receiving', group, highlighted, communicationRules)"
                    [active]="isRelatedWithCell('receiving', group, selected, communicationRules)"></group-card>
                </div>
            </div>
        </div>
        <lightbox-confirm *ngIf="!!selected" title="user.communication.action.confirm.title"
                          [show]="removeConfirmationDisplayed"
                          (onCancel)="removeConfirmationClicked.next('cancel')"
                          (onConfirm)="removeConfirmationClicked.next('confirm')">
            <i class='fa fa-exclamation-triangle is-danger'></i> 
            <span [innerHTML]="'user.communication.remove-communication.confirm.content' | translate: {groupName: groupNameService.getGroupName(selected.group)}"></span>
        </lightbox-confirm>
        
        <group-picker title="services.roles.groups.add"
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
        
        <lightbox-confirm *ngIf="!!selected" title="user.communication.action.confirm.title"
                          [show]="addConfirmationDisplayed"
                          (onCancel)="addConfirmationClicked.next('cancel')"
                          (onConfirm)="addConfirmationClicked.next('confirm')">
            <div class="has-margin-vertical-10">
                <i class='fa fa-exclamation-triangle is-danger'></i> 
                <span [innerHTML]="'user.communication.add-communication.confirm.content' | translate: {groupName: groupNameService.getGroupName(selected.group)}"></span>
            </div>
            <div *ngIf="warningGroupSender" class="has-margin-vertical-10">
                <i class='fa fa-exclamation-triangle is-danger'></i> 
                <span [innerHTML]="'user.communication.add-communication.confirm.users-can-communicate' | translate: {groupName: groupNameService.getGroupName(selected.group)}"></span>
            </div>
            <div *ngIf="warningGroupReceiver" class="has-margin-vertical-10">
                <i class='fa fa-exclamation-triangle is-danger'></i> 
                <span [innerHTML]="'user.communication.add-communication.confirm.users-can-communicate' | translate: {groupName: groupNameService.getGroupName(pickedReceiver)}"></span>
            </div>
        </lightbox-confirm>`,
    styles: [`
        .communication-rules__header {
            color: #2a9cc8;
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
        }
    `, `
        .communication-rules__column.communication-rules__column--receiving,
        .communication-rules__header.communication-rules__header--receiving {
            margin-left: 10px;
        }
    `]
})
export class CommunicationRulesComponent {

    @Input()
    public communicationRules: CommunicationRule[];

    @Input()
    public addCommunicationPickableGroups: GroupModel[];

    public selected: Cell;
    public highlighted: Cell;

    public removeConfirmationDisplayed = false;
    public removeConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();
    public addConfirmationDisplayed = false;
    public addConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public showGroupPicker: boolean = false;
    public warningGroupSender: boolean = false;
    public warningGroupReceiver: boolean = false;
    public pickedReceiver: GroupModel;

    constructor(private communicationRulesService: CommunicationRulesService,
                private notifyService: NotifyService,
                public groupNameService: GroupNameService,
                private bundlesService: BundlesService,
                private changeDetectorRef: ChangeDetectorRef) {
    }

    public select(column: Column, group: GroupModel): void {
        this.selected = { column, group };
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
        return this.communicationRules.map(rule => rule.sender);
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
            .filter(cr => cell.column === 'sending' ? (cr.sender.id === cell.group.id) : cr.receivers.some(g => g.id === cell.group.id))
            .some(cr => cell.column === 'sending' ? cr.receivers.some(g => g.id === group.id) : (cr.sender.id === group.id));
    }

    public removeCommunication(sender: GroupModel, receiver: GroupModel) {
        this.removeConfirmationDisplayed = true;
        this.removeConfirmationClicked.asObservable()
            .first()
            .do(() => this.removeConfirmationDisplayed = false)
            .filter(choice => choice === 'confirm')
            .mergeMap(() => this.communicationRulesService.removeCommunication(sender, receiver))
            .subscribe(() => this.notifyService.success({
                key: 'user.communication.remove-communication.success',
                parameters: {groupName: this.groupNameService.getGroupName(sender)}
            }), () => this.notifyService.error({
                key: 'user.communication.remove-communication.error.content',
                parameters: {groupName: this.groupNameService.getGroupName(sender)}
            }, 'user.communication.remove-communication.error.title'));
    }

    public openGroupPicker(group: GroupModel): void {
        this.showGroupPicker = true;
    }

    public filterGroupPicker = (group: GroupModel) => {
        if (this.selected) {
            return !this.communicationRules
                .find(commRule => commRule.sender.id == this.selected.group.id)
                .receivers.find(receiver => receiver.id === group.id) 
                && group.id != this.selected.group.id;
        }
        return true;
    };

    public onGroupPick(receiver: GroupModel) {
        this.communicationRulesService.checkAddLink(this.selected.group, receiver)
            .subscribe(res => {
                if (res.warning === WARNING_STARTGROUP_USERS_CAN_COMMUNICATE) {
                    this.warningGroupSender = true;
                } else if (res.warning === WARNING_ENDGROUP_USERS_CAN_COMMUNICATE) {
                    this.warningGroupReceiver = true;
                } else if (res.warning === WARNING_BOTH_GROUPS_USERS_CAN_COMMUNICATE) {
                    this.warningGroupSender = true;
                    this.warningGroupReceiver = true;
                }
                this.pickedReceiver = receiver;

                this.addConfirmationDisplayed = true;
                this.changeDetectorRef.markForCheck();

                this.addConfirmationClicked.asObservable()
                    .first()
                    .do(() => this.addConfirmationDisplayed = false)
                    .filter(choice => choice === 'confirm')
                    .mergeMap(() => this.communicationRulesService.createCommunication(this.selected.group, receiver))
                    .subscribe(() => {
                        this.notifyService.success('user.communication.add-communication.success')
                        , () => this.notifyService.error({
                            key: 'user.communication.add-communication.error.content',
                            parameters: {groupName: this.selected.group.name}
                        }, 'user.communication.add-communication.error.title');
                    });
            }, (error: HttpErrorResponse) => {
                if (error.status === 409) {
                    this.notifyService.error('user.communication.add-communication.error.impossible.content'
                        , 'user.communication.add-communication.error.impossible.title')
                } else {
                    this.notifyService.error({
                        key: 'group.internal-communication-rule.change.error.content',
                        parameters: {groupName: this.groupNameService.getGroupName(this.selected.group)}
                    }, 'group.internal-communication-rule.change.error.title')
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

type Column = 'sending' | 'receiving';

interface Cell {
    column: Column;
    group: GroupModel;
}
