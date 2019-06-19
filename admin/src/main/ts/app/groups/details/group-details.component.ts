import { ChangeDetectorRef, Component, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Observable } from 'rxjs/Observable';
import { GroupsStore } from '../groups.store';
import { GroupIdAndInternalCommunicationRule } from './group-internal-communication-rule.resolver';
import { GroupModel, InternalCommunicationRule } from '../../core/store/models';
import { CommunicationRulesService } from '../../users/communication/communication-rules.service';
import { NotifyService, GroupNameService } from '../../core/services';

import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/first';
import 'rxjs/add/operator/filter';
import 'rxjs/add/observable/merge';
import { GroupsService } from '../groups.service';

@Component({
    selector: 'group-detail',
    template: `
        <div class="panel-header is-display-flex has-space-between">
            <span>
                <s5l>members.of.group</s5l>
                {{ groupsStore.group.name }}
            </span>
            <button type="button"
                    *ngIf="groupsStore.group?.type === 'ManualGroup'"
                    (click)="deleteButtonClicked.next(groupsStore.group)"
                    class="lct-group-delete-button">
                <s5l>group.delete.button</s5l>
                <i class="fa fa-trash is-size-5"></i>
            </button>
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
            <i class='fa fa-exclamation-triangle is-danger'></i>
            <span *ngIf="internalCommunicationRule === 'BOTH'; else cannotCommunicateTogetherConfirmMessage" 
                [innerHTML]="'group.internal-communication-rule.remove.confirm.content' | translate: {groupName: groupNameService.getGroupName(groupsStore.group)}"></span>
            <ng-template #cannotCommunicateTogetherConfirmMessage>
                <span [innerHTML]="'group.internal-communication-rule.add.confirm.content' | translate: {groupName: groupNameService.getGroupName(groupsStore.group)}"></span>
            </ng-template>
        </lightbox-confirm>

        <lightbox-confirm lightboxTitle="group.delete.confirm.title"
                          [show]="deleteConfirmationDisplayed"
                          (onCancel)="deleteConfirmationClicked.next('cancel')"
                          (onConfirm)="deleteConfirmationClicked.next('confirm')">
            <span [innerHTML]="'group.delete.confirm.content' | translate: {groupName: groupNameService.getGroupName(groupsStore.group)}"></span>
        </lightbox-confirm>
    `,
    styles: [
        '.lct-communication-rule {cursor: pointer;}',
        '.lct-communication-rule__can-communicate {color: mediumseagreen;}',
        '.lct-communication-rule__cannot-communicate {color: indianred;}',
        '.lct-communication-rule__switch {font-size: 22px;}',
        '.lct-communication-rule__text {font-size: 0.8em;}',
        '.lct-communication-rule__switch, .lct-communication-rule__text {vertical-align: middle;}',
        '.lct-communication-rule__can-communicate .lct-communication-rule__switch {color: mediumseagreen;}',
        '.lct-communication-rule__cannot-communicate .lct-communication-rule__switch {color: indianred;}'
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupDetails implements OnInit, OnDestroy {
    public internalCommunicationRule: InternalCommunicationRule | undefined;
    public showAddUsersLightBox = false;
    public toggleCommunicationRuleClicked: Subject<GroupModel> = new Subject();
    public confirmationDisplayed = false;
    public confirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public deleteSubscription: Subscription;
    public deleteButtonClicked: Subject<GroupModel> = new Subject();
    public deleteConfirmationDisplayed: boolean = false;
    public deleteConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    private changesSubscription: Subscription;

    constructor(public groupsStore: GroupsStore,
                private route: ActivatedRoute,
                private notifyService: NotifyService,
                private communicationRulesService: CommunicationRulesService,
                private cdRef: ChangeDetectorRef,
                public groupNameService: GroupNameService,
                private groupsService: GroupsService,
                private router: Router,
                private activatedRoute: ActivatedRoute) {
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

        this.deleteSubscription = this.deleteButtonClicked
            .mergeMap((group: GroupModel) => this.deleteGroup(group))
            .subscribe();
    }

    ngOnDestroy(): void {
        this.changesSubscription.unsubscribe();
        this.deleteSubscription.unsubscribe();
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

    public deleteGroup(group: GroupModel): Observable<void> {
        this.deleteConfirmationDisplayed = true;
        return this.deleteConfirmationClicked.asObservable()
            .first()
            .do(() => this.deleteConfirmationDisplayed = false)
            .filter(choice => choice === 'confirm')
            .mergeMap(() => this.groupsService.delete(group))
            .do(() => {
                this.notifyService.success({
                    key: 'group.delete.notify.success.content',
                    parameters: {groupName: group.name}
                }, 'group.delete.notify.success.title');
                this.groupsStore.structure.groups.data.splice(
                    this.groupsStore.structure.groups.data.findIndex(g => g.id === group.id)
                    , 1);
                this.router.navigate(['..'], {relativeTo: this.activatedRoute, replaceUrl: false});
                this.cdRef.markForCheck();
            }, (error: HttpErrorResponse) => {
                this.notifyService.error({
                    key: 'group.delete.notify.error.content',
                    parameters: {groupName: group.name}
                }, 'group.delete.notify.error.title');
            });   
    }
}
