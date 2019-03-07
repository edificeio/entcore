import { Component, Input } from '@angular/core';
import { GroupModel } from '../../core/store/models';
import { CommunicationRulesService } from './communication-rules.service';
import { BundlesService } from "sijil";

const css = {
    title: 'lct-group-card-title',
    addCommunicationButton: 'lct-group-card-add-communicaiton-button',
    internalCommunicationSwitch: 'lct-group-card-internal-communication-switch'
};

export const groupCardLocators = {
    title: `.${css.title}`,
    addCommunicationButton: `.${css.addCommunicationButton}`,
    internalCommunicationSwitch: `.${css.internalCommunicationSwitch}`
};

@Component({
    selector: 'group-card',
    template: `
        <div class="group-card">
            <div class="group-card__title ${css.title}">{{getGroupName(group)}}</div>
            <div class="group-card__actions">
                <button class="group-card__action-add-communication ${css.addCommunicationButton}">{{ 'group.card.add-communication-button' | translate }} <i class="fa fa-plus"></i></button>
            </div>
            <hr class="group-card__separator"/>
            <span class="group-card__actions-on-self group-card__actions-on-self--can-communicate"
                      *ngIf="group.internalCommunicationRule === 'BOTH'; else cannotCommunicateTogether;">
                <s5l class="group-card__switch-label">group.details.members.can.communicate</s5l> <i
                    class="${css.internalCommunicationSwitch} group-card__switch fa fa-toggle-on"
                    (click)="toggleInternalCommunicationRule()"></i>
            </span>
            <ng-template #cannotCommunicateTogether>
                <span class="group-card__actions-on-self group-card__actions-on-self--cannot-communicate">
                    <s5l class="group-card__switch-label">group.details.members.cannot.communicate</s5l> <i
                        class="${css.internalCommunicationSwitch} group-card__switch fa fa-toggle-off"
                        (click)="toggleInternalCommunicationRule()"></i>
                </span>
            </ng-template>
        </div>`,
    styles: [`
        .group-card {
            background-color: #00a4d3;
            font-size: 14px;
            padding: 10px;
            margin: 10px 0;
            width: 340px;
            color: white;
            box-sizing: border-box;
            box-shadow: 1px 1px 5px #AAA;
        }
    `, `
        .group-card__title {
            font-size: 16px;
            margin-bottom: 15px;
        }
    `, `
        .group-card__actions {
        }
    `, `
        .group-card__action-add-communication {
            background: #ff6624;
            height: 34px;
            line-height: 34px;
            padding: 0 10px;
            border-radius: 0;
            border: 0;
            color: white;
        }
    `, `
        .group-card__action-add-communication i {
            float: none;
        }
    `, `
        .group-card__separator {
            color: white;
            border-style: solid;
            margin: 15px 0;
        }
    `, `
        .group-card__switch {
            font-size: 22px;
            padding-right: 15px;
            cursor: pointer;
        }
    `, `
        .group-card__actions-on-self {
            display: flex;
            align-items: center;
            padding-bottom: 10px;
        }
    `, `
        .group-card__actions-on-self.group-card__actions-on-self--can-communicate .group-card__switch {
            color: green;
        }
    `, `
        .group-card__actions-on-self.group-card__actions-on-self--cannot-communicate .group-card__switch {
            color: red;
        }
    `]
})
export class GroupCardComponent {

    @Input()
    group: GroupModel;

    constructor(private communicationRulesService: CommunicationRulesService, private bundlesService: BundlesService) {
    }

    public toggleInternalCommunicationRule() {
        this.communicationRulesService
            .toggleInternalCommunicationRule(this.group)
            .subscribe();
    }

    public getGroupName(group: GroupModel): string {
        if (group.type === 'ManualGroup') {
            return group.name;
        }

        if (group.type === 'ProfileGroup') {
            if (group.filter && group.classes && group.classes.length > 0) {
                return this.bundlesService.translate(`group.card.class.${group.filter}`, {name: group.classes[0].name});
            } else if (group.filter && group.structures && group.structures.length > 0) {
                return this.bundlesService.translate(`group.card.structure.${group.filter}`, {name: group.structures[0].name});
            }
        }

        // Defaulting to the console v1 behaviour
        const indexOfSeparation = group.name.lastIndexOf('-');
        if (indexOfSeparation < 0) {
            return group.name;
        }
        return `${this.bundlesService.translate(group.name.slice(0, indexOfSeparation))}-${this.bundlesService.translate(group.name.slice(indexOfSeparation + 1))}`;
    }
}
