import { ChangeDetectorRef, ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { GroupsStore } from '../../store'

@Component({
    selector: 'group-detail',
    template: `
        <div class="panel-header">
            <span><s5l>members.of.group</s5l> {{ groupsStore.group.name }}</span>
        </div>

        <div class="padded">
            <button (click)="showAddUsersLightBox = true">
                <s5l>group.details.add.users</s5l>
            </button>

            <light-box class="inner-list" [show]="showAddUsersLightBox" (onClose)="showAddUsersLightBox = false">
                <group-manage-users>
                </group-manage-users>
            </light-box>

            <group-users-list [selectedGroup]="groupsStore.group">
                <strong class="badge">
                    {{ groupsStore.group?.users?.length }}
                    {{ 'members' | translate:{ count: groupsStore.group?.users?.length } | lowercase }}
                </strong>
            </group-users-list>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupDetail implements OnInit, OnDestroy {

    private groupSubscriber : Subscription

    constructor(
        private groupsStore: GroupsStore,
        private route: ActivatedRoute,
        private cdRef : ChangeDetectorRef){}

    ngOnInit(): void {
        this.groupSubscriber = this.route.params.subscribe(params => {
            if(params["groupId"]) {
                this.groupsStore.group = this.groupsStore.structure.groups.data.find(g => g.id === params["groupId"])
                this.cdRef.markForCheck()
            }
        })
    }

    ngOnDestroy(): void {
        this.groupSubscriber.unsubscribe()
    }
}
