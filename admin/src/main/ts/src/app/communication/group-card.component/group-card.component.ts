import {Component, EventEmitter, Input, Output} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute} from '@angular/router';
import {GroupModel} from '../../core/store/models/group.model';
import {CommunicationRulesService} from '../communication-rules.service';
import {getStructureIdOfGroup, getStructureOfGroup} from '../communication-rules.component/communication-rules.component';
import {Subject} from 'rxjs';
import {filter, first, mergeMap, tap} from 'rxjs/operators';
import { SpinnerService } from 'src/app/core/services/spinner.service';
import { NotifyService } from 'src/app/core/services/notify.service';
import { GroupNameService } from 'src/app/core/services/group-name.service';

const css = {
    title: 'lct-group-card-title',
    subtitle: 'lct-group-card-subtitle',
    label: 'lct-group-card-label',
    structure: 'lct-group-card-structure',
    viewMembersButton: 'lct-group-card-view-members-button',
    addCommunicationButton: 'lct-group-card-add-communication-button',
    removeCommunicationButton: 'lct-group-card-remove-communication-button',
    internalCommunicationSwitch: 'lct-group-card-internal-communication-switch'
};

export const groupCardLocators = {
    title: `.${css.title}`,
    subtitle: `.${css.subtitle}`,
    label: `.${css.label}`,
    structure: `.${css.structure}`,
    viewMembersButton: `.${css.viewMembersButton}`,
    addCommunicationButton: `.${css.addCommunicationButton}`,
    removeCommunicationButton: `.${css.removeCommunicationButton}`,
    internalCommunicationSwitch: `.${css.internalCommunicationSwitch}`
};

@Component({
    selector: 'ode-group-card',
    templateUrl: './group-card.component.html',
    styleUrls: ['./group-card.component.css']
})
export class GroupCardComponent {

    @Input()
    group: GroupModel;

    @Input()
    selectable = false;

    @Input()
    active = false;

    @Input()
    selected = false;

    @Input()
    highlighted = false;

    @Input()
    manageable = false;

    @Input()
    communicationRuleManageable = false;

    @Output()
    clickAddCommunication: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();

    @Output()
    clickOnRemoveCommunication: EventEmitter<void> = new EventEmitter<void>();

    confirmationDisplayed = false;
    $confirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    groupTypeRouteMapping: Map<string, string> = new Map<string, string>()
        .set('ManualGroup', 'manualGroup')
        .set('ProfileGroup', 'profileGroup')
        .set('FunctionalGroup', 'functionalGroup')
        .set('FunctionGroup', 'functionGroup');


    constructor(
        private spinner: SpinnerService,
        private route: ActivatedRoute,
        private notifyService: NotifyService,
        private communicationRulesService: CommunicationRulesService,
        public groupNameService: GroupNameService) {
    }

    public toggleInternalCommunicationRule() {
        this.confirmationDisplayed = true;
        this.$confirmationClicked.asObservable()
            .pipe(
                first(),
                tap(() => this.confirmationDisplayed = false),
                filter(choice => choice === 'confirm'),
                mergeMap(() => this.communicationRulesService.toggleInternalCommunicationRule(this.group))
            )
            .subscribe(() => this.notifyService.success('group.internal-communication-rule.change.success'),
                (error: HttpErrorResponse) => {
                    if (error.status === 409) {
                        this.notifyService.error({
                            key: 'group.internal-communication-rule.change.conflict.content',
                            parameters: {groupName: this.groupNameService.getGroupName(this.group)}
                        }, 'group.internal-communication-rule.change.conflict.title');
                    } else {
                        this.notifyService.error({
                            key: 'group.internal-communication-rule.change.error.content',
                            parameters: {groupName: this.groupNameService.getGroupName(this.group)}
                        }, 'group.internal-communication-rule.change.error.title');
                    }
                });
    }

    public viewMembers(group: GroupModel) {
        window.open(`/admin/${getStructureIdOfGroup(group)}/groups/${this.groupTypeRouteMapping.get(group.type)}/${group.id}/details`, '_blank');
    }

    getStructureNameOfGroup(group: GroupModel): string {
        return getStructureOfGroup(group).name;
    }
}
