import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Observable } from 'rxjs/Observable';
import { GroupsStore } from '../groups.store';
import { GroupIdAndInternalCommunicationRule } from './group-internal-communication-rule.resolver';
import { GroupModel, InternalCommunicationRule } from '../../core/store/models';
import { CommunicationRulesService } from '../../users/communication/communication-rules.service';
import { NotifyService } from '../../core/services';

import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/first';
import 'rxjs/add/operator/filter';
import 'rxjs/add/observable/merge';

@Component({
    selector: 'group-detail',
    template: `
        <div class="panel-header">
            <span><s5l>members.of.group</s5l>
                {{ groupsStore.group.name }}</span>
        </div>

        <div class="padded">
            <button (click)="showLightBox()" *ngIf="groupsStore.group?.type === 'ManualGroup'">
                <s5l>group.details.add.users</s5l>
            </button>

            <lightbox class="inner-list" [show]="showAddUsersLightBox" (onClose)="closeLightBox()">
                <group-manage-users (close)="closeLightBox()"></group-manage-users>
            </lightbox>

            <group-users-list [users]="groupsStore.group?.users">
                <span class="lct-communication-rule"
                      *ngIf="internalCommunicationRule && groupsStore.group?.type === 'ManualGroup'"
                      (click)="toggleCommunicationRuleClicked.next(groupsStore.group)">
                    <span class="lct-communication-rule__can-communicate"
                          *ngIf="internalCommunicationRule === 'BOTH'; else cannotCommunicateTogether;">
                        <s5l class="lct-communication-rule__text">group.details.members.can.communicate</s5l> <i
                            class="lct-communication-rule__switch fa fa-toggle-on"></i>
                    </span>
                    <ng-template #cannotCommunicateTogether>
                        <span class="lct-communication-rule__cannot-communicate">
                            <s5l class="lct-communication-rule__text">group.details.members.cannot.communicate</s5l> <i
                                class="lct-communication-rule__switch fa fa-toggle-off"></i>
                        </span>
                    </ng-template>
                </span>
            </group-users-list>
        </div>
        <lightbox-confirm *ngIf="groupsStore && groupsStore.group" lightboxTitle="group.internal-communication-rule.change.confirm.title"
                          [show]="confirmationDisplayed"
                          (onCancel)="confirmationClicked.next('cancel')"
                          (onConfirm)="confirmationClicked.next('confirm')">
            <span [innerHTML]="'group.internal-communication-rule.change.confirm.content' | translate: {groupName: groupsStore.group.name}"></span>
        </lightbox-confirm>
    `,
    styles: [
        '.lct-communication-rule {cursor: pointer;}',
        '.lct-communication-rule__can-communicate {color: green;}',
        '.lct-communication-rule__cannot-communicate {color: red;}',
        '.lct-communication-rule__switch {font-size: 22px;}',
        '.lct-communication-rule__text {font-size: 0.8em;}',
        '.lct-communication-rule__switch, .lct-communication-rule__text {vertical-align: middle;}',
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupDetails implements OnInit, OnDestroy {
    public internalCommunicationRule: InternalCommunicationRule | undefined;
    public showAddUsersLightBox = false;
    public toggleCommunicationRuleClicked: Subject<GroupModel> = new Subject();
    public confirmationDisplayed = false;
    public confirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    private changesSubscription: Subscription;

    constructor(public groupsStore: GroupsStore,
                private route: ActivatedRoute,
                private notifyService: NotifyService,
                private communicationRulesService: CommunicationRulesService,
                private cdRef: ChangeDetectorRef) {
    }

    ngOnInit(): void {
        const rulesChangesObserver = Observable.merge(
            this.route.data
                .map((data: { rule: GroupIdAndInternalCommunicationRule }) => data.rule),
            this.toggleCommunicationRuleClicked
                .switchMap(group => this.toggleCommunicationBetweenMembers(group))
                .filter(res => res.groupId === this.groupsStore.group.id) // don't affect the UI if we changed the current group
        )
            .map((data: GroupIdAndInternalCommunicationRule) => data.internalCommunicationRule)
            .do(rule => this.internalCommunicationRule = rule);

        const groupChangesObserver = this.route.params
            .filter(params => params['groupId'])
            .do(params =>
                this.groupsStore.group = this.groupsStore.structure.groups.data
                    .find(g => g.id === params['groupId'])
            );

        this.changesSubscription = Observable
            .merge(rulesChangesObserver, groupChangesObserver)
            .subscribe(() => this.cdRef.markForCheck());

    }

    ngOnDestroy(): void {
        this.changesSubscription.unsubscribe();
    }

    showLightBox() {
        this.showAddUsersLightBox = true;
        document.body.style.overflowY = 'hidden';
    }

    closeLightBox() {
        this.showAddUsersLightBox = false;
        document.body.style.overflowY = 'auto';
    }

    toggleCommunicationBetweenMembers(group: GroupModel): Observable<GroupIdAndInternalCommunicationRule> {
        this.confirmationDisplayed = true;
        return this.confirmationClicked.asObservable()
            .first()
            .do(() => this.confirmationDisplayed = false)
            .filter(choice => choice === 'confirm')
            .mergeMap(() => this.communicationRulesService.toggleInternalCommunicationRule({id: group.id, internalCommunicationRule: this.internalCommunicationRule} as GroupModel))
            .do(
                () => this.notifyService.success({
                    key: 'group.internal-communication-rule.change.success',
                    parameters: {groupName: group.name}
                }),
                (error: HttpErrorResponse) => {
                    if (error.status === 409) {
                        this.notifyService.error({
                            key: 'group.internal-communication-rule.change.conflict.content',
                            parameters: {groupName: group.name}
                        }, 'group.internal-communication-rule.change.conflict.title')
                    } else {
                        this.notifyService.error({
                            key: 'group.internal-communication-rule.change.error.content',
                            parameters: {groupName: group.name}
                        }, 'group.internal-communication-rule.change.error.title')
                    }
                })
            .map(internalCommunicationRule => ({groupId: group.id, internalCommunicationRule}));
    }
}
