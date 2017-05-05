import { ActivatedRoute } from '@angular/router'
import { Subscription } from 'rxjs'
import { GroupsDataService } from '../../../services/groups'
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core'

@Component({
    selector: 'group-detail',
    template: `
        <div class="padded">
            <group-users-list [selectedGroup]="dataService.group">
                <strong class="badge">
                    {{ dataService.group?.users?.length }}
                    {{ 'members' | translate:{ count: dataService.group?.users?.length } | lowercase }}
                </strong>
            </group-users-list>
        </div>
    `
})
export class GroupDetailComponent implements OnInit, OnDestroy {

    private groupSubscriber : Subscription

    constructor(
        private dataService: GroupsDataService,
        private route: ActivatedRoute,
        private cdRef : ChangeDetectorRef){}

    ngOnInit(): void {
       this.groupSubscriber = this.route.params.subscribe(params => {
            if(params["groupId"]) {
                this.dataService.group = this.dataService.structure.groups.data.find(g => g.id === params["groupId"])
                this.cdRef.markForCheck()
            }
        })
    }

    ngOnDestroy(): void {
        this.groupSubscriber.unsubscribe()
    }

}