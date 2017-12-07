import { ChangeDetectorRef, ChangeDetectionStrategy, Component, 
    OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'

import { GroupsStore } from '../groups.store'

@Component({
    selector: 'group-detail',
    template: `
        <div class="panel-header">
            <span><s5l>members.of.group</s5l> {{ groupsStore.group.name }}</span>
        </div>

        <div class="padded">
            <button (click)="showLightBox()" 
                *ngIf="groupsStore.group?.type === 'ManualGroup'">
                <s5l>group.details.add.users</s5l>
            </button>

            <lightbox class="inner-list" [show]="showAddUsersLightBox" 
                (onClose)="closeLightBox()">
                <group-manage-users (close)="closeLightBox()"></group-manage-users>
            </lightbox>

            <group-users-list [users]="groupsStore.group?.users">
            </group-users-list>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupDetails implements OnInit, OnDestroy {

    showAddUsersLightBox: boolean = false

    private groupSubscriber : Subscription

    constructor(
        public groupsStore: GroupsStore,
        private route: ActivatedRoute,
        private cdRef : ChangeDetectorRef){}

    ngOnInit(): void {
        this.groupSubscriber = this.route.params.subscribe(params => {
            if(params["groupId"]) {
                this.groupsStore.group = this.groupsStore.structure.groups.data.find(
                    g => g.id === params["groupId"])
                this.cdRef.markForCheck()
            }
        })
    }

    ngOnDestroy(): void {
        this.groupSubscriber.unsubscribe()
    }

    showLightBox() {
        this.showAddUsersLightBox = true
        document.body.style.overflowY = 'hidden'
    }

    closeLightBox() {
        this.showAddUsersLightBox = false
        document.body.style.overflowY = 'auto'
    }
}
