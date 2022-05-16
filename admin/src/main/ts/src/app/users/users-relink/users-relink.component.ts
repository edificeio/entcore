import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';
import { UsersStore } from '../users.store';
import { UserListService } from 'src/app/core/services/userlist.service';
import { UsersListComponent } from '../users-list/users-list.component';
import { UserlistFiltersService } from 'src/app/core/services/userlist.filters.service';
import { SpinnerService } from 'ngx-ode-ui';
import { BundlesService } from 'ngx-ode-sijil';


@Component({
    selector: 'ode-users-relink',
    templateUrl: './users-relink.component.html',
    providers: [UsersStore, UserListService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersRelinkComponent extends UsersListComponent {
    constructor(
        injector: Injector,
        public usersStore: UsersStore,
        protected listFilters: UserlistFiltersService,
        protected spinner: SpinnerService,
        protected sijilService: BundlesService
    ) {
        super(injector, usersStore, listFilters, spinner, sijilService);
    }
}
