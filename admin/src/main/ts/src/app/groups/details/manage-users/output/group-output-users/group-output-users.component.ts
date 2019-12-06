import { ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { UserListService } from 'src/app/core/services/userlist.service';
import { UserModel } from '../../../../../core/store/models/user.model';
import { GroupsStore } from '../../../../groups.store';


@Component({
    selector: 'ode-group-output-users',
    templateUrl: './group-output-users.component.html',
    providers: [ UserListService ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupOutputUsersComponent extends OdeComponent{
    @Input() model: UserModel[] = [];
    @Output() onDelete: EventEmitter<any> = new EventEmitter();

    // list elements stored by store pipe in list component
    // (takes filters in consideration)
    storedElements: UserModel[] = [];

    // Users selected by enduser
    selectedUsers: UserModel[] = [];

    constructor(private groupsStore: GroupsStore,
                injector: Injector,
                public userLS: UserListService,
                private spinner: SpinnerService,
                private ns: NotifyService) {
        super(injector);
    }

    selectUser(u): void {
        if (this.selectedUsers.indexOf(u) === -1) {
            this.selectedUsers.push(u);
        } else {
            this.selectedUsers = this.selectedUsers.filter(su => su.id !== u.id);
        }
    }

    isSelected = (user: UserModel) => {
        return this.selectedUsers.indexOf(user) > -1;
    }

    selectAll(): void {
        this.selectedUsers = this.storedElements;
    }

    deselectAll(): void {
        this.selectedUsers = [];
    }

    removeUsers(): void {
        this.spinner.perform('group-manage-users',
            this.groupsStore.group.removeUsers(this.selectedUsers)
                .then(() => {
                    this.groupsStore.group.users = this.groupsStore.group.users
                        .filter(gu => this.selectedUsers.indexOf(gu) === -1);
                    this.onDelete.emit();
                    this.selectedUsers = [];
                    this.ns.success('notify.group.manage.users.removed.content');
                    this.changeDetector.markForCheck();
                })
                .catch((err) => {
                    this.ns.error('notify.group.manage.users.removed.error.content'
                        , 'notify.group.manage.users.removed.error.title'
                        , err);
                })
        );
    }
}
