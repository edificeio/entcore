import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { trim } from 'ngx-ode-ui';
import { merge, Observable, Subject } from 'rxjs';
import { filter, first, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { GroupNameService } from 'src/app/core/services/group-name.service';
import { NotifyService } from 'src/app/core/services/notify.service';
import { GroupModel, InternalCommunicationRule } from 'src/app/core/store/models/group.model';
import { CommunicationRulesService } from '../../../communication/communication-rules.service';
import { GroupsService } from '../../groups.service';
import { GroupsStore } from '../../groups.store';
import { GroupIdAndInternalCommunicationRule } from '../group-internal-communication-rule.resolver';


@Component({
    selector: 'ode-group-detail',
    templateUrl: './group-details.component.html',
    styleUrls: ['./group-details.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupDetailsComponent extends OdeComponent implements OnInit, OnDestroy {
    public internalCommunicationRule: InternalCommunicationRule | undefined;
    public showAddUsersLightBox = false;
    public $toggleCommunicationRuleClicked: Subject<GroupModel> = new Subject();
    public confirmationDisplayed = false;
    public $confirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public $deleteButtonClicked: Subject<GroupModel> = new Subject();
    public deleteConfirmationDisplayed = false;
    public $deleteConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public $renameButtonClicked: Subject<{}> = new Subject();
    public renameLightboxDisplayed = false;
    public renameConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public groupNewName: string;

    constructor(public groupsStore: GroupsStore,
                private notifyService: NotifyService,
                private communicationRulesService: CommunicationRulesService,
                public groupNameService: GroupNameService,
                private groupsService: GroupsService,
                injector: Injector) {
                super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
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

        this.subscriptions.add(merge(rulesChangesObserver, groupChangesObserver)
            .subscribe(() => this.changeDetector.markForCheck()));

        this.subscriptions.add(this.$deleteButtonClicked
        .pipe(
            mergeMap((group: GroupModel) => this.deleteGroup(group))
        )
            .subscribe());

        this.subscriptions.add(this.$renameButtonClicked
        .pipe(
            mergeMap(() => this.renameGroup())
        )
        .subscribe());

        this.groupNewName = this.groupsStore.group.name;
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
                this.router.navigate(['../..'], {relativeTo: this.route, replaceUrl: false});
                this.changeDetector.markForCheck();
            }, (error: HttpErrorResponse) => {
                this.notifyService.error({
                    key: 'group.delete.notify.error.content',
                    parameters: {groupName: group.name}
                }, 'group.delete.notify.error.title');
            })
        );
    }

    public openGroupCommunication(group: GroupModel) {
        this.router.navigate([group.id, 'communication'], {relativeTo: this.route.parent});
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
