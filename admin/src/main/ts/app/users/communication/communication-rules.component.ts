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
                <span class="communication-rules__header communication-rules__header--sending">{{ 'user.communication.groups-of-user' | translate }}</span>
                <span class="communication-rules__header communication-rules__header--receiving">{{ 'user.communication.groups-that-user-can-communicate-with' | translate }}</span>
        </div>
        <div class="communication-rules__columns">
            <div class="communication-rules__column communication-rules__column--sending ${css.sendingColumn}">
                <div class="group ${css.group}" *ngFor="let sender of getSenders(); trackBy: trackByGroupId">
                    <group-card [group]="sender"></group-card>
                </div>
            </div>
            <div class="communication-rules__column communication-rules__column--receiving ${css.receivingColumn}">
                <div class="group ${css.group}" *ngFor="let receiver of getReceivers(); trackBy: trackByGroupId">
                    <group-card [group]="receiver"></group-card>
                </div>
            </div>
        </div>`,
    styles: [`
        .communication-rules__header {
            color: #2a9cc8;
            font-size: 20px;
        }`, `
        .communication-rules__headers, .communication-rules__columns {
            display: flex;
            align-items: center;
        }`, `
        .communication-rules__header, .communication-rules__column {
            flex-grow: 1;
            flex-basis: 0;
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
    communicationRules: CommunicationRule[];

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
