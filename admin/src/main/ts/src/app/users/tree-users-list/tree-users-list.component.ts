import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { SpinnerService } from 'ngx-ode-ui';
import { routing } from '../../core/services/routing.service';
import { UserlistFiltersService } from '../../core/services/userlist.filters.service';
import { UserListService } from '../../core/services/userlist.service';
import { UsersStore } from '../users.store';

@Component({
    selector: 'ode-tree-users-list',
    templateUrl: './tree-users-list.component.html',
    providers: [UsersStore, UserListService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TreeUsersListComponent extends OdeComponent {
    
    constructor(
        injector: Injector,
        public usersStore: UsersStore,
        protected listFilters: UserlistFiltersService,
        protected spinner: SpinnerService,
        public userListService: UserListService
    ) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.usersStore.structure = data.structure;
                this.changeDetector.detectChanges();
            }
        }));
        this.subscriptions.add(this.userListService.$updateSubject.subscribe(() => this.changeDetector.markForCheck()));
        this.subscriptions.add(this.usersStore.$onchange.subscribe((field) => {
            if (field === 'user') {
                this.changeDetector.markForCheck();
            }
        }));
    }

    closeCompanion() {
        this.router.navigate(['../users/tree-list'], {relativeTo: this.route}).then(() => {
            this.usersStore.user = null;
        });
    }

    openUserDetail(user) {
        this.usersStore.user = user;
        this.spinner.perform('portal-content', this.router.navigate([user.id, 'details'], {relativeTo: this.route}));
    }

    openCompanionView(view) {
        this.router.navigate([view], {relativeTo: this.route});
    }
}
