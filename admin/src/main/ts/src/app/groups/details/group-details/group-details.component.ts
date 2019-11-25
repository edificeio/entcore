import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {merge, Observable, Subject, Subscription} from 'rxjs';
import {GroupsStore} from '../../groups.store';
import {GroupIdAndInternalCommunicationRule} from '../group-internal-communication-rule.resolver';
import {GroupModel, InternalCommunicationRule} from '../../../core/store/models';
import {CommunicationRulesService} from '../../../communication/communication-rules.service';
import {GroupNameService, NotifyService} from '../../../core/services';
import {filter, first, map, mergeMap, switchMap, tap} from 'rxjs/operators';

import {GroupsService} from '../../groups.service';
import {trim} from '../../../shared/utils/string';

@Component({
    selector: 'ode-group-detail',
    templateUrl: './group-details.component.html',
    styleUrls: ['./group-details.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupDetailsComponent implements OnInit, OnDestroy {
    public internalCommunicationRule: InternalCommunicationRule | undefined;
    public showAddUsersLightBox = false;
    public $toggleCommunicationRuleClicked: Subject<GroupModel> = new Subject();
    public confirmationDisplayed = false;
    public $confirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public deleteSubscription: Subscription;
    public $deleteButtonClicked: Subject<GroupModel> = new Subject();
    public deleteConfirmationDisplayed = false;
    public $deleteConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public renameSubscription: Subscription;
    public $renameButtonClicked: Subject<{}> = new Subject();
    public renameLightboxDisplayed = false;
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
        const rulesChangesObserver = merge(
            this.route.data
                .pipe(map((data: { rule: GroupIdAndInternalCommunicationRule }) => data.rule)),

            this.$toggleCommunicationRuleClicked
                .pipe(
                    switchMap(group => this.toggleCommunicationBetweenMembers(group)),
                    filter(res => res.groupId === this.groupsStore.group.id)
                ) // don't affect the UI if we changed the current group
        )
        .pipe(
            map((data: GroupIdAndInternalCommunicationRule) => data.internalCommunicationRule),
            tap(rule => this.internalCommunicationRule = rule)
        );

        const groupChangesObserver = this.route.params
            .pipe(
                filter(params => params.groupId),
                tap(params =>
                this.groupsStore.group = this.groupsStore.structure.groups.data
                    .find(g => g.id === params.groupId)
                )
            );

        this.changesSubscription = merge(rulesChangesObserver, groupChangesObserver)
            .subscribe(() => this.cdRef.markForCheck());

        this.deleteSubscription = this.$deleteButtonClicked
        .pipe(
            mergeMap((group: GroupModel) => this.deleteGroup(group))
        )
            .subscribe();

        this.renameSubscription = this.$renameButtonClicked
            .pipe(
                mergeMap(() => this.renameGroup())
            )
            .subscribe();

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
        return this.$confirmationClicked.asObservable()
        .pipe(
            first(),
            tap(() => this.confirmationDisplayed = false),
            filter(choice => choice === 'confirm'),
            mergeMap(() => this.communicationRulesService.toggleInternalCommunicationRule({
                id: group.id,
                internalCommunicationRule: this.internalCommunicationRule
            } as GroupModel)),
            tap(
                () => this.notifyService.success({
                    key: 'group.internal-communication-rule.change.success',
                    parameters: {groupName: group.name}
                }),
                (error: HttpErrorResponse) => {
                    if (error.status === 409) {
                        this.notifyService.error({
                            key: 'group.internal-communication-rule.change.conflict.content',
                            parameters: {groupName: group.name}
                        }, 'group.internal-communication-rule.change.conflict.title');
                    } else {
                        this.notifyService.error({
                            key: 'group.internal-communication-rule.change.error.content',
                            parameters: {groupName: group.name}
                        }, 'group.internal-communication-rule.change.error.title');
                    }
                }),
            map(internalCommunicationRule => ({groupId: group.id, internalCommunicationRule}))
        );
    }

    public deleteGroup(group: GroupModel): Observable<void> {
        this.deleteConfirmationDisplayed = true;
        return this.$deleteConfirmationClicked.asObservable()
        .pipe(
            first(),
            tap(() => this.deleteConfirmationDisplayed = false),
            filter(choice => choice === 'confirm'),
            mergeMap(() => this.groupsService.delete(group)),
            tap(() => {
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
            })
        );
    }

    public openGroupCommunication(group: GroupModel) {
        this.router.navigate([group.id, 'communication'], {relativeTo: this.activatedRoute.parent});
    }

    public renameGroup(): Observable<void> {
        this.renameLightboxDisplayed = true;
        return this.renameConfirmationClicked.asObservable()
        .pipe(
            first(),
            tap(() => this.renameLightboxDisplayed = false),
            filter(choice => choice === 'confirm'),
            mergeMap(() => this.groupsService.update({id: this.groupsStore.group.id, name: this.groupNewName})),
            tap(() => {
                this.notifyService.success('group.rename.notify.success.content'
                    , 'group.rename.notify.success.title');
            }, (error: HttpErrorResponse) => {
                this.notifyService.error('group.rename.notify.error.content'
                    , 'group.rename.notify.error.title');
            })
        );
    }

    public onGroupNameBlur(name: string): void {
        this.groupNewName = trim(name);
    }
}
