import { Component, EventEmitter, Input, Output } from '@angular/core';
import { GroupModel } from '../../core/store/models';
import { CommunicationRulesService } from './communication-rules.service';
import { ActivatedRoute } from '@angular/router';
import { NotifyService, SpinnerService } from '../../core/services';
import { HttpErrorResponse } from '@angular/common/http';
import { GroupNameService } from './group-name.service';

const css = {
    title: 'lct-group-card-title',
    viewMembersButton: 'lct-group-card-view-members-button',
    addCommunicationButton: 'lct-group-card-add-communicaiton-button',
    removeCommunicationButton: 'lct-group-card-remove-communicaiton-button',
    internalCommunicationSwitch: 'lct-group-card-internal-communication-switch'
};

export const groupCardLocators = {
    title: `.${css.title}`,
    viewMembersButton: `.${css.viewMembersButton}`,
    addCommunicationButton: `.${css.addCommunicationButton}`,
    removeCommunicationButton: `.${css.removeCommunicationButton}`,
    internalCommunicationSwitch: `.${css.internalCommunicationSwitch}`
};

@Component({
    selector: 'group-card',
    template: `
        <div class="group-card"
        [ngClass]="{'group-card--active': active, 'group-card--selected': selected, 'group-card--highlighted': highlighted}">
            <div class="group-card__title ${css.title}">
                <span class="group-card__title-label">{{groupNameService.getGroupName(group)}}</span>
                <i *ngIf="groupTypeRouteMapping.has(group.type)" (click)="viewMembers(group)" class="group-card__title-icon ${css.viewMembersButton} fa fa-users" [title]="'group.card.view-members-button' | translate"></i>
            </div>
            <div class="group-card__actions">
                <button
                    class="group-card__action-add-communication ${css.addCommunicationButton}"
                    (click)="$event.stopPropagation();">
                    {{ 'group.card.add-communication-button' | translate }} <i class="fa fa-plus"></i>
                </button>
                <button
                    class="group-card__action-remove-communication ${css.removeCommunicationButton}"
                    (click)="clickOnRemoveCommunication.emit(); $event.stopPropagation();">
                    {{ 'group.card.remove-communication-button' | translate }} <i class="fa fa-minus"></i>
                </button>
            </div>
            <hr class="group-card__separator" [hidden]="!active"/>
            <span class="group-card__actions-on-self group-card__actions-on-self--can-communicate"
                      *ngIf="group.internalCommunicationRule === 'BOTH'; else cannotCommunicateTogether;">
                <s5l class="group-card__switch-label">group.details.members.can.communicate</s5l> <i
                    class="${css.internalCommunicationSwitch} group-card__switch fa fa-toggle-on"
                    (click)="toggleInternalCommunicationRule(); $event.stopPropagation();"></i>
            </span>
            <ng-template #cannotCommunicateTogether>
                <span class="group-card__actions-on-self group-card__actions-on-self--cannot-communicate">
                    <s5l class="group-card__switch-label">group.details.members.cannot.communicate</s5l> <i
                        class="${css.internalCommunicationSwitch} group-card__switch fa fa-toggle-off"
                        (click)="toggleInternalCommunicationRule(); $event.stopPropagation();"></i>
                </span>
            </ng-template>
        </div>`,
    styles: [`
        .group-card {
            background-color: #c0c0c0;
            color: black;
            font-size: 14px;
            padding: 10px;
            margin: 5px 0;
            width: 340px;
            box-sizing: border-box;
            cursor: pointer;
            transition: box-shadow 125ms ease;
        }

        .group-card.group-card--active {
            background-color: #2a9cc8;
            color: white;
            box-shadow: 1px 1px 5px #aaa;
        }

        .group-card.group-card--active, .group-card.group-card--selected {
            cursor: default;
        }

        .group-card.group-card--highlighted {
            box-shadow: 1px 1px 5px #aaa;
        }

        .group-card.group-card--active.group-card--highlighted {
            box-shadow: 3px 3px 8px 2px #aaa;
        }
    `, `
        .group-card__title {
            font-size: 16px;
            display: flex;
            align-items: center;
        }

        .group-card--active .group-card__title {
            margin-bottom: 15px;
        }
    `, `
        .group-card__title-label {
            flex-grow: 1;
        }
    `, `
        .group-card__title-icon {
            display: none;
            cursor: pointer;
            transition: color 125ms ease;
        }

        .group-card--active .group-card__title-icon {
            display: initial;
        }

        .group-card__title-icon:hover {
            color: #ff8352;
        }
    `, `
        .group-card__actions {
            display: none;
        }

        .group-card--active .group-card__actions {
            display: initial;
        }
    `, `
        .group-card__action-remove-communication, .group-card__action-add-communication {
            background: #ff6624;
            height: 34px;
            line-height: 34px;
            padding: 0 10px;
            border-radius: 0;
            border: 0;
            color: white;
        }

        .group-card__action-add-communication, .group-card--selected .group-card__action-remove-communication {
            display: none;
        }

        .group-card--selected .group-card__action-add-communication {
            display: initial;
        }
    `, `
        .group-card__action-remove-communication i, .group-card__action-add-communication i {
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
            display: none;
        }

        .group-card--active .group-card__actions-on-self {
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

    @Input()
    active: boolean = false;

    @Input()
    selected: boolean = false;

    @Input()
    highlighted: boolean = false;

    @Output()
    clickOnRemoveCommunication: EventEmitter<void> = new EventEmitter<void>();

    groupTypeRouteMapping: Map<string, string> = new Map<string, string>()
        .set('ManualGroup', 'manual')
        .set('ProfileGroup', 'profile')
        .set('FunctionalGroup', 'functional');

    constructor(
        private spinner: SpinnerService,
        private route: ActivatedRoute,
        private notifyService: NotifyService,
        private communicationRulesService: CommunicationRulesService,
        public groupNameService: GroupNameService) {
    }

    public toggleInternalCommunicationRule() {
        this.communicationRulesService
            .toggleInternalCommunicationRule(this.group)
            .subscribe(() => this.notifyService.success({
                    key: 'group.internal-communication-rule.change.success',
                    parameters: {groupName: this.groupNameService.getGroupName(this.group)}
                }),
                (error: HttpErrorResponse) => {
                    if (error.status === 409) {
                        this.notifyService.error({
                            key: 'group.internal-communication-rule.change.conflict.content',
                            parameters: {groupName: this.groupNameService.getGroupName(this.group)}
                        }, 'group.internal-communication-rule.change.conflict.title')
                    } else {
                        this.notifyService.error({
                            key: 'group.internal-communication-rule.change.error.content',
                            parameters: {groupName: this.groupNameService.getGroupName(this.group)}
                        }, 'group.internal-communication-rule.change.error.title')
                    }
                });
    }

    public viewMembers(group: GroupModel) {
        window.open(`/admin/${group.structures[0].id}/groups/${this.groupTypeRouteMapping.get(group.type)}/${group.id}`, '_blank');
    }
}
