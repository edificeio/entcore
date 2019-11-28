import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnInit, Output} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs';

import {UserModel} from '../../../../core/store/models/user.model';
import {GroupsStore} from '../../../groups.store';

@Component({
    selector: 'ode-group-manage-users',
    templateUrl: './group-manage-users.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupManageUsersComponent implements OnInit {
    @Output()
    closeEmitter: EventEmitter<void> = new EventEmitter<void>();

    inputUsers: UserModel[] = [];

    private groupSubscriber: Subscription;

    constructor(private cdRef: ChangeDetectorRef,
                public groupsStore: GroupsStore,
                private route: ActivatedRoute) {}

    ngOnInit(): void {
        if (this.groupsStore.structure.users.data
            && this.groupsStore.structure.users.data.length < 1) {
            this.groupsStore.structure.users.sync().then(() => {
                this.populateInputUsers();
            });
        } else {
            this.populateInputUsers();
        }

        this.groupSubscriber = this.route.params.subscribe(params => {
            if (params.groupId) {
                this.populateInputUsers();
            }
        });
    }

    populateInputUsers(): void {
        this.inputUsers = this.filterUsers(this.groupsStore.structure.users.data
            , this.groupsStore.group.users);
    }

    private filterUsers(sUsers: UserModel[], gUsers: UserModel[]): UserModel[] {
        return sUsers.filter(u => gUsers.map(x => x.id).indexOf(u.id) === -1);
    }
}
