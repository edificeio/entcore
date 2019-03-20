import { Component, Input } from '@angular/core';
import { GroupModel } from '../../core/store/models';

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
                <span class="communication-rules__header">{{ 'user.communication.groups-of-user' | translate }}</span>
                <span class="communication-rules__header">{{ 'user.communication.groups-that-user-can-communicate-with' | translate }}</span>
        </div>
        <div class="communication-rules__columns">
            <div class="communication-rules__column ${css.sendingColumn}">
                <div class="group ${css.group}" *ngFor="let group of getSenders(); trackBy: trackByGroupId">
                    <group-card
                    (click)="select('sending', group)"
                    (mouseenter)="highlight('sending', group, selected)"
                    (mouseleave)="resetHighlight()"
                    [group]="group"
                    [selected]="isSelected('sending', group, selected)"
                    [highlighted]="isRelatedWithCell('sending', group, highlighted, communicationRules)"
                    [active]="isRelatedWithCell('sending', group, selected, communicationRules)"></group-card>
                </div>
            </div>
            <div class="communication-rules__column ${css.receivingColumn}">
                <div class="group ${css.group}" *ngFor="let group of getReceivers(); trackBy: trackByGroupId">
                    <group-card
                    (click)="select('receiving', group)"
                    (mouseenter)="highlight('receiving', group, selected)"
                    (mouseleave)="resetHighlight()"
                    [group]="group"
                    [selected]="isSelected('receiving', group, selected)"
                    [highlighted]="isRelatedWithCell('receiving', group, highlighted, communicationRules)"
                    [active]="isRelatedWithCell('receiving', group, selected, communicationRules)"></group-card>
                </div>
            </div>
        </div>`,
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
    `]
})
export class CommunicationRulesComponent {

    @Input()
    communicationRules: CommunicationRule[];

    selected: Cell;
    highlighted: Cell;

    public select(column: Column, group: GroupModel): void {
        this.selected = {column, group};
        this.resetHighlight();
    }

    public highlight(column: Column, group: GroupModel, selected: Cell): void {
        if (!selected || column !== selected.column || group.id !== selected.group.id) {
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
