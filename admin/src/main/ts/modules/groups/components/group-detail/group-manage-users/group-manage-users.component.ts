import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { UserModel } from '../../../../../store/models'
import { GroupsStore } from '../../../store'

@Component({
    selector: 'group-manage-users',
    template: `
        <spinner-cube class="component-spinner" waitingFor="group-manage-users"></spinner-cube>

        <div class="padded">
            <h2>
                <span><s5l>group.manage.users</s5l></span>
            </h2>

            <div class="container">
                <group-input-users [model]="structureUsers"></group-input-users>

                <group-output-users [model]="groupsStore.group.users"></group-output-users>
            </div>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupManageUsers implements OnInit {
    private structureUsers: UserModel[] = []
    private groupSubscriber : Subscription

    constructor(private cdRef: ChangeDetectorRef,
        private groupsStore: GroupsStore,
        private route: ActivatedRoute){}

    ngOnInit(): void {
        this.groupsStore.structure.users.sync().then(() => {
            this.structureUsers = this.groupsStore.structure.users.data
            this.cdRef.detectChanges()
        })

        this.groupSubscriber = this.route.params.subscribe(params => {
            if(params["groupId"]) {
                this.cdRef.detectChanges()
            }
        })
    }
}
