import { Component, EventEmitter, Injector, OnInit, Output, ViewChild, ElementRef } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { Subscription } from 'rxjs';
import { UserModel } from '../../../../core/store/models/user.model';
import { GroupsStore } from '../../../groups.store';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { GroupInputUsersComponent } from '../input/group-input-users/group-input-users.component';
import { GroupOutputUsersComponent } from '../output/group-output-users/group-output-users.component';

@Component({
    selector: 'ode-group-manage-users',
    templateUrl: './group-manage-users.component.html',
    styleUrls: ['./group-manage-users.component.scss'],

})
export class GroupManageUsersComponent extends OdeComponent implements OnInit {
    @Output()
    closeEmitter: EventEmitter<void> = new EventEmitter<void>();

    @ViewChild(GroupInputUsersComponent, { static: false }) groupInputUsersComponent: GroupInputUsersComponent;
    @ViewChild(GroupOutputUsersComponent, { static: false }) groupOutputUsersComponent: GroupOutputUsersComponent;

    inputUsers: UserModel[];

    inputUsersSelected: UserModel[];
    outputUsersSelected: UserModel[];

    private groupSubscriber: Subscription;

    constructor(public groupsStore: GroupsStore,
                injector: Injector,
                private spinner: SpinnerService,
                private notifyService: NotifyService) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
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

    addUsers(): void {
        this.spinner.perform('group-manage-users',
            this.groupsStore.group.addUsers(this.inputUsersSelected)
                .then(() => {
                    this.groupsStore.group.users = this.groupsStore.group.users.concat(this.inputUsersSelected);
                    this.inputUsers = this.inputUsers.filter(u => this.inputUsersSelected.indexOf(u) === -1);
                    this.inputUsersSelected = [];
                    this.groupInputUsersComponent.selectedUsers = [];
                    this.notifyService.success('notify.group.manage.users.added.content');
                    this.changeDetector.markForCheck();
                })
                .catch((err) => {
                    this.notifyService.error('notify.group.manage.users.added.error.content'
                        , 'notify.group.manage.users.added.error.title', err);
                })
        );
    }

    removeUsers(): void {
        this.spinner.perform('group-manage-users',
            this.groupsStore.group.removeUsers(this.outputUsersSelected)
                .then(() => {
                    this.groupsStore.group.users = this.groupsStore.group.users.filter(gu => this.outputUsersSelected.indexOf(gu) === -1);
                    this.inputUsers = this.inputUsers.concat(this.outputUsersSelected);
                    this.outputUsersSelected = [];
                    this.groupOutputUsersComponent.selectedUsers = [];
                    this.notifyService.success('notify.group.manage.users.removed.content');
                    this.changeDetector.markForCheck();
                })
                .catch((err) => {
                    this.notifyService.error('notify.group.manage.users.removed.error.content'
                        , 'notify.group.manage.users.removed.error.title'
                        , err);
                })
        );
    }

    onInputSelectUsers(users: UserModel[]): void {
        this.inputUsersSelected = users;
    }

    onOutputSelectUsers(users: UserModel[]): void {
        this.outputUsersSelected = users;
    }

    populateInputUsers(): void {
        this.inputUsers = this.filterUsers(this.groupsStore.structure.users.data, this.groupsStore.group.users);
        this.changeDetector.markForCheck();
    }

    private filterUsers(sUsers: UserModel[], gUsers: UserModel[]): UserModel[] {
        return sUsers.filter(u => gUsers.map(x => x.id).indexOf(u.id) === -1);
    }

    isAddUsersButtonDisabled(): boolean {
        return !this.inputUsersSelected || this.inputUsersSelected && this.inputUsersSelected.length === 0;
    }

    isRemoveUsersButtonDisabled(): boolean {
        return !this.outputUsersSelected || this.outputUsersSelected && this.outputUsersSelected.length === 0;
    }
}
