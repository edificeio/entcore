import { Component, OnInit, ChangeDetectionStrategy
    , ChangeDetectorRef } from '@angular/core'
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
                <group-input-users [model]="inputUsers"></group-input-users>

                <group-output-users [model]="groupsStore.group.users" (onDelete)="populateInputUsers()"></group-output-users>
            </div>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupManageUsers implements OnInit {
    private inputUsers: UserModel[] = []
    private groupSubscriber : Subscription

    constructor(private cdRef: ChangeDetectorRef,
        private groupsStore: GroupsStore,
        private route: ActivatedRoute){}

    ngOnInit(): void {
        if (this.groupsStore.structure.users.data 
            && this.groupsStore.structure.users.data.length < 1) {
            this.groupsStore.structure.users.sync().then(() => {
                this.populateInputUsers()
            })
        } else {
            this.populateInputUsers()
        }

        this.groupSubscriber = this.route.params.subscribe(params => {
            if(params["groupId"]) {
                this.populateInputUsers()
            }
        })
    }

    private populateInputUsers(): void {
        this.inputUsers = this.filterUsers(this.groupsStore.structure.users.data
            , this.groupsStore.group.users)
    }

    /**
    * @returns UserModel[] from structure users minus group users
    */
    private filterUsers(sUsers: UserModel[], gUsers: UserModel[]): UserModel[] {
        return sUsers.filter(u => gUsers.map(x => x.id).indexOf(u.id) === -1)
    }
}
