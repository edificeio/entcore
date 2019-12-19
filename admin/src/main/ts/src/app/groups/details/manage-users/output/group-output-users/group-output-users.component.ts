import { Component, EventEmitter, Injector, Input, Output } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { UserListService } from 'src/app/core/services/userlist.service';
import { UserModel } from '../../../../../core/store/models/user.model';

@Component({
    selector: 'ode-group-output-users',
    templateUrl: './group-output-users.component.html',
    styleUrls: ['./group-output-users.component.scss'],
    providers: [ UserListService ]
})
export class GroupOutputUsersComponent extends OdeComponent {
    @Input() model: UserModel[] = [];
    @Output() selectUsers: EventEmitter<UserModel[]> = new EventEmitter();

    // list elements stored by store pipe in list component
    // (takes filters in consideration)
    storedElements: UserModel[] = [];

    // Users selected by enduser
    selectedUsers: UserModel[] = [];

    constructor(injector: Injector,
                public userLS: UserListService) {
        super(injector);
    }

    selectUser(u): void {
        if (this.selectedUsers.indexOf(u) === -1) {
            this.selectedUsers.push(u);
        } else {
            this.selectedUsers = this.selectedUsers.filter(su => su.id !== u.id);
        }
        this.selectUsers.emit(this.selectedUsers);
    }

    isSelected = (user: UserModel) => {
        return this.selectedUsers.indexOf(user) > -1;
    }

    selectAll(): void {
        this.selectedUsers = this.storedElements;
        this.selectUsers.emit(this.selectedUsers);
    }

    deselectAll(): void {
        this.selectedUsers = [];
        this.selectUsers.emit(this.selectedUsers);
    }
}
