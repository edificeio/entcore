import { Component, EventEmitter, Input, Output } from '@angular/core';
import { GroupModel } from '../../core/store/models';
import { CommunicationRulesService } from './communication-rules.service';
import { ActivatedRoute } from '@angular/router';
import { NotifyService, SpinnerService, GroupNameService } from '../../core/services';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/first';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/mergeMap';

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
                    (click)="clickAddCommunication.emit(group);">
                    {{ 'group.card.add-communication-button' | translate }} <i class="fa fa-plus"></i>
                </button>
                <button
                    class="group-card__action-remove-communication ${css.removeCommunicationButton}"
                    (click)="clickOnRemoveCommunication.emit(); $event.stopPropagation();">
                    {{ 'group.card.remove-communication-button' | translate }} <i class="fa fa-minus"></i>
                </button>
            </div>
            <hr class="group-card__separator"/>
            <span class="group-card__actions-on-self group-card__actions-on-self--can-communicate"
                      *ngIf="group.internalCommunicationRule === 'BOTH'; else cannotCommunicateTogether;">
                <s5l class="group-card__switch-label">group.details.members.can.communicate</s5l>
                <i class="${css.internalCommunicationSwitch} group-card__switch group-card__switch--clickable fa fa-toggle-on"
                   *ngIf="active"
                   (click)="toggleInternalCommunicationRule(); $event.stopPropagation();"></i>
                <i class="${css.internalCommunicationSwitch} group-card__switch fa fa-toggle-on"
                   *ngIf="!active"></i>
            </span>
            <ng-template #cannotCommunicateTogether>
                <span class="group-card__actions-on-self group-card__actions-on-self--cannot-communicate">
                    <s5l class="group-card__switch-label">group.details.members.cannot.communicate</s5l>
                    <i class="${css.internalCommunicationSwitch} group-card__switch group-card__switch--clickable fa fa-toggle-off"
                       *ngIf="active"
                       (click)="toggleInternalCommunicationRule(); $event.stopPropagation();"></i>
                    <i class="${css.internalCommunicationSwitch} group-card__switch fa fa-toggle-off"
                       *ngIf="!active"></i>
                </span>
            </ng-template>
        </div>
        <lightbox-confirm lightboxTitle="group.internal-communication-rule.change.confirm.title"
                          [show]="confirmationDisplayed"
                          (onCancel)="confirmationClicked.next('cancel')"
                          (onConfirm)="confirmationClicked.next('confirm')">
            <div class="has-vertical-margin-10">
                <i class='fa fa-exclamation-triangle is-danger'></i>
                <span *ngIf="group.internalCommunicationRule === 'BOTH'; else cannotCommunicateTogetherConfirmMessage" 
                    [innerHTML]="'group.internal-communication-rule.remove.confirm.content' | translate: {groupName: groupNameService.getGroupName(this.group)}"></span>
                <ng-template #cannotCommunicateTogetherConfirmMessage>
                    <span [innerHTML]="'group.internal-communication-rule.add.confirm.content' | translate: {groupName: groupNameService.getGroupName(this.group)}"></span>
                </ng-template>
            </div>
        </lightbox-confirm>`,
    styles: [`
        .group-card {
            color: #5b6472;
            border: 1px solid rgba(0, 0, 0, 0.25);
            background-color: #eaedf2;
            border-radius: 5px;
            font-size: 14px;
            padding: 10px;
            margin: 5px 0;
            width: 340px;
            box-sizing: border-box;
            cursor: pointer;
            transition: box-shadow 125ms ease;
        }

        .group-card.group-card--active {
            background-color: white;
            box-shadow: 1px 1px 5px rgba(0,0,0,0.25);
            border-color: #ff8352;
        }

        .group-card.group-card--active,
        .group-card.group-card--selected {
            cursor: default;
        }

        .group-card.group-card--highlighted {
            background-color: #fff2ed;
            box-shadow: 1px 1px 5px rgba(0,0,0,0.25);
            border-color: #ff8352;
            color: #ff8352;
        }

        .group-card.group-card--active.group-card--highlighted {
            box-shadow: 1px 1px 5px rgba(0,0,0,0.25);
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
        .group-card__action-remove-communication,
        .group-card__action-add-communication {
            background: #ff8352;
            height: 34px;
            line-height: 34px;
            padding: 0 10px;
            border: 0;
            color: white;
        }

        .group-card__action-remove-communication:hover,
        .group-card__action-add-communication:hover {
            background: #ff6624;
        }

        .group-card__action-add-communication,
        .group-card--selected .group-card__action-remove-communication {
            display: none;
        }

        .group-card--selected .group-card__action-add-communication {
            display: initial;
        }
    `, `
        .group-card__action-remove-communication i,
        .group-card__action-add-communication i {
            float: none;
        }
    `, `
        .group-card__separator {
            margin: 15px 0;
        }
    `, `
        .group-card__switch {
            font-size: 22px;
            padding-right: 15px;
        }
        .group-card__switch.group-card__switch--clickable {
            cursor: pointer;
        }
    `, `
        .group-card__actions-on-self {
            display: flex;
            align-items: center;
            padding-bottom: 10px;
        }
    `, `
        .group-card__actions-on-self.group-card__actions-on-self--can-communicate .group-card__switch.group-card__switch--clickable {
            color: mediumseagreen;
        }
    `, `
        .group-card__actions-on-self.group-card__actions-on-self--cannot-communicate .group-card__switch.group-card__switch--clickable {
            color: indianred;
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

    confirmationDisplayed = false;
    confirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();
    @Output()
    clickAddCommunication: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();

    groupTypeRouteMapping: Map<string, string> = new Map<string, string>()
        .set('ManualGroup', 'manual')
        .set('ProfileGroup', 'profile')
        .set('FunctionalGroup', 'functional');

    @Output()
    clickOnRemoveCommunication: EventEmitter<void> = new EventEmitter<void>();


    constructor(
        private spinner: SpinnerService,
        private route: ActivatedRoute,
        private notifyService: NotifyService,
        private communicationRulesService: CommunicationRulesService,
        public groupNameService: GroupNameService) {
    }

    public toggleInternalCommunicationRule() {
        this.confirmationDisplayed = true;
        this.confirmationClicked.asObservable()
            .first()
            .do(() => this.confirmationDisplayed = false)
            .filter(choice => choice === 'confirm')
            .mergeMap(() => this.communicationRulesService.toggleInternalCommunicationRule(this.group))
            .subscribe(() => this.notifyService.success('group.internal-communication-rule.change.success'),
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
