import { OdeComponent } from './../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, Input, Injector } from '@angular/core';

import {UserModel} from '../../../core/store/models/user.model';
import {UserListService} from '../../../core/services/userlist.service';

@Component({
    selector: 'ode-group-users-list',
    templateUrl: './group-users-list.component.html',
    styleUrls: ['./group-users-list.component.scss'],
    providers: [UserListService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupUsersListComponent extends OdeComponent {
    constructor(injector: Injector, public userLS: UserListService) {
        super(injector);
    }

    @Input()
    users: UserModel[];

    selectUser(user: UserModel) {
        if (user.structures.length > 0) {
            this.router.navigate(['admin', user.structures[0].id, 'users', user.id, 'details']);
        }
    }
}
