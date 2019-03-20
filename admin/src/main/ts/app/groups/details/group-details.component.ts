import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/observable/merge';

import { GroupsStore } from '../groups.store';
import { GroupIdAndInternalCommunicationRule } from './group-internal-communication-rule.resolver';
import { InternalCommunicationRule } from '../../core/store/models';

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
                      (click)="toggleCommunicationRuleClicked.next({groupId: groupsStore.group.id, internalCommunicationRule: internalCommunicationRule})">
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
    public toggleCommunicationRuleClicked: Subject<GroupIdAndInternalCommunicationRule> = new Subject();

    private changesSubscription: Subscription;

    constructor(public groupsStore: GroupsStore,
                private http: HttpClient,
                private route: ActivatedRoute,
                private cdRef: ChangeDetectorRef) {
    }

    ngOnInit(): void {
        const rulesChangesObserver = Observable.merge(
            this.route.data
                .map((data: { rule: GroupIdAndInternalCommunicationRule }) => data.rule),
            this.toggleCommunicationRuleClicked
                .switchMap(data => this.toggleCommunicationBetweenMembers(data.groupId, data.internalCommunicationRule))
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

    toggleCommunicationBetweenMembers(groupId: string, internalCommunicationRule: InternalCommunicationRule): Observable<GroupIdAndInternalCommunicationRule> {
        let request: Observable<{ number: number }>;
        const direction: InternalCommunicationRule = internalCommunicationRule === 'BOTH' ? 'NONE' : 'BOTH';

        if (direction === 'BOTH') {
            request = this.http.post<{ number: number }>(`/communication/group/${groupId}`, {direction});
        } else {
            request = this.http.request<{ number: number }>('delete', `/communication/group/${groupId}`, {body: {direction: 'BOTH'}});
        }

        return request.map(() => ({groupId, internalCommunicationRule: direction}));
    }
}
