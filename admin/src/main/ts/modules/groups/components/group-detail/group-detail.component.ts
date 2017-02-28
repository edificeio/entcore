import { ActivatedRoute } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { GroupsStore } from '../../store'
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core'

@Component({
    selector: 'group-detail',
    template: `
        <div class="padded">
            <group-users-list [selectedGroup]="groupsStore.group">
                <strong class="badge">
                    {{ groupsStore.group?.users?.length }}
                    {{ 'members' | translate:{ count: groupsStore.group?.users?.length } | lowercase }}
                </strong>
            </group-users-list>
        </div>
    `
})
export class GroupDetailComponent implements OnInit, OnDestroy {

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