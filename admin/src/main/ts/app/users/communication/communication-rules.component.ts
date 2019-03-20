import { Component, Input } from '@angular/core';
import { GroupModel } from '../../core/store/models';

const css = {
    group: 'lct-user-communication-group'
};

export const communicationRulesLocators = {
    group: `.${css.group}`
};

@Component({
    selector: 'communication-rules',
    template: `
        <span class="title">{{ 'user.communication.groups-of-user' | translate }}</span>
        <div class="group ${css.group}" *ngFor="let sender of getSenders(); trackBy: trackByGroupId">
            <group-card [group]="sender"></group-card>
        </div>`,
    styles: [`
        .title {
            color: #2a9cc8;
            font-size: 20px;
        }`]
})
export class CommunicationRulesComponent {

    @Input()
    communicationRules: CommunicationRule[];

    public getSenders(): GroupModel[] {
        return this.communicationRules.map(rule => rule.sender);
    }

    public trackByGroupId(index: number, group: GroupModel): string {
        return group.id;
    }
}

export interface CommunicationRule {
    sender: GroupModel,
    receivers: GroupModel[]
}
