import { Component, Input } from '@angular/core';
import { GroupModel } from '../../core/store/models';
import { CommunicationRulesService } from './communication-rules.service';
import { NotifyService } from '../../core/services';
import { GroupNameService } from './group-name.service';
import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/first';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/mergeMap';

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
                    [active]="isRelatedWithCell('sending', group, selected, communicationRules)"></group-card>
                </div>
            </div>
            <div class="communication-rules__column communication-rules__column--receiving ${css.receivingColumn}">
                <div class="group ${css.group}" *ngFor="let group of getReceivers(); trackBy: trackByGroupId">
                    <group-card
                    (click)="select('receiving', group)"
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
        <lightbox-confirm title="user.communication.remove-communication.confirm.title"
                          [show]="confirmationDisplayed"
                          (onCancel)="confirmationClicked.next('cancel')"
                          (onConfirm)="confirmationClicked.next('confirm')">
            <span [innerHTML]="'user.communication.remove-communication.confirm.content' | translate"></span>
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

    public selected: Cell;
    public highlighted: Cell;

    public confirmationDisplayed = false;
    public confirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    constructor(private communicationRulesService: CommunicationRulesService,
                private notifyService: NotifyService,
                private groupNameService: GroupNameService) {
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
            .filter(cr => (cr.sender.id === cell.group.id) || cr.receivers.some(g => g.id === cell.group.id))
            .some(cr => (cr.sender.id === group.id) || cr.receivers.some(g => g.id === group.id));
    }

    public removeCommunication(sender: GroupModel, receiver: GroupModel) {
        this.confirmationDisplayed = true;
        this.confirmationClicked.asObservable()
            .first()
            .do(() => this.confirmationDisplayed = false)
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
