import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Observable } from 'rxjs/Observable';
import { GroupsStore } from '../groups.store';
import { GroupIdAndInternalCommunicationRule } from './group-internal-communication-rule.resolver';
import { GroupModel, InternalCommunicationRule } from '../../core/store/models';
import { CommunicationRulesService } from '../../communication/communication-rules.service';
import { GroupNameService, NotifyService } from '../../core/services';

import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/first';
import 'rxjs/add/operator/filter';
import 'rxjs/add/observable/merge';
import { GroupsService } from '../groups.service';
import { trim } from '../../shared/utils/string';

@Component({
    selector: 'group-detail',
    template: `
        <div class="panel-header is-display-flex has-space-between">
            <div>
                <span>
                    {{ groupsStore.group.name }}
                </span>
            </div>
            <div>
                <button type="button"
                        *ngIf="groupsStore.group?.type === 'ManualGroup'"
                        (click)="renameButtonClicked.next()"
                        class="lct-group-edit-button">
                    <s5l>group.rename.button</s5l>
                    <i class="fa fa-pencil is-size-5"></i>
                </button>
                <button type="button"
                        *ngIf="groupsStore.group?.type === 'ManualGroup'"
                        (click)="deleteButtonClicked.next(groupsStore.group)"
                        class="lct-group-delete-button">
                    <s5l>group.delete.button</s5l>
                    <i class="fa fa-trash is-size-5"></i>
                </button>
            </div>
        </div>

        <div class="padded">
            <button (click)="showLightBox()" *ngIf="groupsStore.group?.type === 'ManualGroup'">
                <s5l>group.details.add.users</s5l>
            </button>

            <lightbox class="inner-list" [show]="showAddUsersLightBox" (onClose)="closeLightBox()">
                <group-manage-users (close)="closeLightBox()"></group-manage-users>
            </lightbox>
            
            <button class="button--with-icon" (click)="openGroupCommunication(groupsStore.group)">
                <s5l>group.details.button.comm.rules</s5l>
                <i class="fa fa-podcast"></i>
            </button>


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
        <lightbox-confirm *ngIf="groupsStore && groupsStore.group"
                          lightboxTitle="group.internal-communication-rule.change.confirm.title"
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
        
        <lightbox [show]="renameLightboxDisplayed" (onClose)="this.renameLightboxDisplayed = false">
            <h2><s5l>group.rename.lightbox.title</s5l></h2>
            <form #renameForm="ngForm">
                <form-field label="group.rename.lightbox.name">
                    <input type="text" [(ngModel)]="groupNewName" name="groupNewName"
                            required pattern=".*\\S+.*" #groupNewNameInput="ngModel"
                            (blur)="onGroupNameBlur(groupNewName)">
                    <form-errors [control]="groupNewNameInput"></form-errors>
                </form-field>
                
                <div class="is-display-flex has-flex-end">
                    <button type="button" class="cancel" (click)="renameConfirmationClicked.next('cancel')">
                        {{ 'cancel' | translate }}
                    </button>
                    <button type="button" class="confirm has-left-margin-10" 
                        (click)="renameConfirmationClicked.next('confirm')"
                        [disabled]="renameForm.pristine || renameForm.invalid">
                        {{ 'confirm' | translate }}
                    </button>
                </div>
            </form>
        </lightbox>
    `,
    styles: [
        '.lct-communication-rule {cursor: pointer;}',
        '.lct-communication-rule__can-communicate {color: mediumseagreen;}',
        '.lct-communication-rule__cannot-communicate {color: indianred;}',
        '.lct-communication-rule__switch {font-size: 22px;}',
        '.lct-communication-rule__text {font-size: 0.8em;}',
        '.lct-communication-rule__switch, .lct-communication-rule__text {vertical-align: middle;}',
        '.lct-communication-rule__can-communicate .lct-communication-rule__switch {color: mediumseagreen;}',
        '.lct-communication-rule__cannot-communicate .lct-communication-rule__switch {color: indianred;}',
        '.button--with-icon {align-items: center; display: inline-flex;}'
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

    public renameSubscription: Subscription;
    public renameButtonClicked: Subject<{}> = new Subject();
    public renameLightboxDisplayed: boolean = false;
    public renameConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    private changesSubscription: Subscription;

    public groupNewName: string;

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

        this.renameSubscription = this.renameButtonClicked
            .mergeMap(() => this.renameGroup())
            .subscribe()
        
        this.groupNewName = this.groupsStore.group.name;
    }

    ngOnDestroy(): void {
        this.changesSubscription.unsubscribe();
        this.deleteSubscription.unsubscribe();
        this.renameSubscription.unsubscribe();
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
            .mergeMap(() => this.communicationRulesService.toggleInternalCommunicationRule({
                id: group.id,
                internalCommunicationRule: this.internalCommunicationRule
            } as GroupModel))
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
                this.router.navigate(['../..'], {relativeTo: this.activatedRoute, replaceUrl: false});
                this.cdRef.markForCheck();
            }, (error: HttpErrorResponse) => {
                this.notifyService.error({
                    key: 'group.delete.notify.error.content',
                    parameters: {groupName: group.name}
                }, 'group.delete.notify.error.title');
            });
    }

    public openGroupCommunication(group: GroupModel) {
        this.router.navigate([group.id, 'communication'], {relativeTo: this.activatedRoute.parent});
    }

    public renameGroup(): Observable<void> {
        this.renameLightboxDisplayed = true;
        return this.renameConfirmationClicked.asObservable()
            .first()
            .do(() => this.renameLightboxDisplayed = false)
            .filter(choice => choice === 'confirm')
            .switchMap(() => this.groupsService.update({id: this.groupsStore.group.id, name: this.groupNewName} as GroupModel))
            .do(() => {
                this.notifyService.success('group.rename.notify.success.content'
                    , 'group.rename.notify.success.title');
            }, (error: HttpErrorResponse) => {
                this.notifyService.error('group.rename.notify.error.content'
                    , 'group.rename.notify.error.title');
            });
    }

    public onGroupNameBlur(name: string): void {
        this.groupNewName = trim(name);
    }
}
