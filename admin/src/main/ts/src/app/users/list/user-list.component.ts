import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import {Subscription} from 'rxjs';

import {UserlistFiltersService, UserListService} from '../../core/services';
import {UserModel} from '../../core/store/models';

import {UsersStore} from '../users.store';
import {Router} from '@angular/router';

@Component({
    selector: 'ode-user-list',
    templateUrl: './user-list.component.html',
    styleUrls: ['./user-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserListComponent implements OnInit, OnDestroy, AfterViewChecked {

    private filtersUpdatesSubscriber: Subscription;
    private userUpdatesSubscriber: Subscription;
    private storeSubscriber: Subscription;
    nbUser: number;

    @Input() userlist: UserModel[] = [];

    @Input() listCompanion: string;
    @Output('listCompanionChange') companionChange: EventEmitter<string> = new EventEmitter<string>();

    // Selection
    @Input() selectedUser: UserModel;
    @Output('selectedUserChange') onselect: EventEmitter<UserModel> = new EventEmitter<UserModel>();

    constructor(
        private cdRef: ChangeDetectorRef,
        private usersStore: UsersStore,
        public userListService: UserListService,
        public listFiltersService: UserlistFiltersService,
        private router: Router) {}

    ngOnInit() {
        this.filtersUpdatesSubscriber = this.listFiltersService.$updateSubject.subscribe(() => this.cdRef.markForCheck());
        this.userUpdatesSubscriber = this.userListService.$updateSubject.subscribe(() => this.cdRef.markForCheck());
        this.storeSubscriber = this.usersStore.$onchange.subscribe((field) => {
            if (field === 'user') {
                this.cdRef.markForCheck();
            }
        });
        this.nbUser = this.userlist.length;
    }

    ngOnDestroy() {
        this.filtersUpdatesSubscriber.unsubscribe();
        this.userUpdatesSubscriber.unsubscribe();
        this.storeSubscriber.unsubscribe();
    }

    ngAfterViewChecked() {
        // called to update list nbUser after filters update
        this.cdRef.markForCheck();
    }

    isSelected = (user: UserModel) => {
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
