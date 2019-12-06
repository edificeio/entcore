import { AfterViewChecked, ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { UserlistFiltersService } from 'src/app/core/services/userlist.filters.service';
import { UserListService } from 'src/app/core/services/userlist.service';
import { UserModel } from '../../core/store/models/user.model';
import { UsersStore } from '../users.store';

@Component({
    selector: 'ode-user-list',
    templateUrl: './user-list.component.html',
    styleUrls: ['./user-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserListComponent extends OdeComponent implements OnInit, OnDestroy, AfterViewChecked {

    nbUser: number;

    @Input() userlist: UserModel[] = [];

    @Input() listCompanion: string;
    @Output() companionChange: EventEmitter<string> = new EventEmitter<string>();

    // Selection
    @Input() selectedUser: UserModel;
    @Output() onselect: EventEmitter<UserModel> = new EventEmitter<UserModel>();

    constructor(
        private usersStore: UsersStore,
        public userListService: UserListService,
        public listFiltersService: UserlistFiltersService,
        injector: Injector) {
            super(injector);
        }

    ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.listFiltersService.$updateSubject.subscribe(() => this.changeDetector.markForCheck()));
        this.subscriptions.add(this.userListService.$updateSubject.subscribe(() => this.changeDetector.markForCheck()));
        this.subscriptions.add(this.usersStore.$onchange.subscribe((field) => {
            if (field === 'user') {
                this.changeDetector.markForCheck();
            }
        }));
        this.nbUser = this.userlist.length;
    }

    ngAfterViewChecked() {
        // called to update list nbUser after filters update
        this.changeDetector.markForCheck();
    }

    isSelected = (user: UserModel): boolean => {
        return this.selectedUser && user && this.selectedUser.id === user.id;
    }

    refreshListCount(list): void {
        this.nbUser = list.length;
    }

    filtersOn(): boolean {
        return this.listFiltersService.filters.some(f => f.outputModel && f.outputModel.length > 0);
    }

    isFilterSelected(): boolean {
        return this.router.url.indexOf('/users/filter') > -1;
    }
}
