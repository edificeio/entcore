import { Component, Input, Output, OnInit, OnDestroy, EventEmitter,
    ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core'
import { Subscription } from 'rxjs/Subscription'

import { UserListService, UserlistFiltersService } from '../../core/services'
import { UserModel } from '../../core/store/models'

import { UsersStore } from '../users.store';

@Component({
    selector: 'user-list',
    template: `
    <list
        [model]="userlist"
        [filters]="listFiltersService.getFormattedFilters()"
        [inputFilter]="userListService.filterByInput"
        [sort]="userListService.sorts"
        searchPlaceholder="search.user"
        noResultsLabel="list.results.no.users"
        [ngClass]="setStyles"
        [limit]="userListService.limit"
        [listScroll]="userListService.listScroll"
        (inputChange)="userListService.inputFilter = $event"
        (onSelect)="selectedUser = $event; onselect.emit($event)">
        <div toolbar class="user-toolbar">
             <i class="fa" aria-hidden="true"
                [ngClass]="{
                    'fa-sort-alpha-asc': userListService.sortsMap.alphabetical.sort === '+',
                    'fa-sort-alpha-desc': userListService.sortsMap.alphabetical.sort === '-',
                    'selected': userListService.sortsMap.alphabetical.selected
                }"
                [tooltip]="'sort.alphabetical' | translate" position="top"
                (click)="userListService.changeSorts('alphabetical')"></i>
            <i class="fa" aria-hidden="true"
                [ngClass]="{
                    'fa-sort-amount-asc': userListService.sortsMap.profile.sort === '+',
                    'fa-sort-amount-desc': userListService.sortsMap.profile.sort === '-',
                    'selected': userListService.sortsMap.profile.selected
                }"
                [tooltip]="'sort.profile' | translate" position="top"
                (click)="userListService.changeSorts('profile')"></i>
            <i class="fa fa-filter toolbar-right" aria-hidden="true"
                [tooltip]="'filters' | translate" position="top"
                (click)="companionChange.emit('filter')"></i>
            <strong class="badge">{{ userlist.length }} <s5l>list.results.users</s5l></strong>
        </div>

        <ng-template let-item>
            <span class="display-name">
                {{item.lastName?.toUpperCase()}} {{item.firstName}}
            </span>
            <span class="icons">
                <i class="fa fa-lock" 
                    *ngIf="item.code && item.code?.length > 0"
                    [tooltip]="'user.icons.tooltip.inactive' | translate"></i>
                <i class="fa fa-ban" 
                    *ngIf="item.blocked"
                    [tooltip]="'user.icons.tooltip.blocked' | translate"></i>
                <i class="fonticon duplicates" 
                    *ngIf="item.duplicates && item.duplicates?.length > 0"
                    [tooltip]="'user.icons.tooltip.duplicated' | translate"></i>
                <i class="fa fa-times-circle" 
                    *ngIf="item.deleteDate"
                    [tooltip]="'user.icons.tooltip.deleted' | translate"></i>
                <i class="fonticon waiting-predelete" 
                    *ngIf="!item.deleteDate && item.disappearanceDate"
                    [tooltip]="'user.icons.tooltip.disappeared' | translate"></i>
            </span>
            <i class="profile" [ngClass]="item.type">{{item.type | translate}}</i>
        </ng-template>
    </list>
    `,
    styles: [`
        .user-toolbar {
            padding: 15px;
            font-size: 1.2em;
        }
        .user-toolbar i {
            cursor: pointer;
        }
    `],

    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserList implements OnInit, OnDestroy {

    private filtersUpdatesSubscriber: Subscription;
    private userUpdatesSubscriber: Subscription;
    private storeSubscriber: Subscription;

    @Input() userlist: UserModel[] = [];

    @Input() listCompanion: string;
    @Output("listCompanionChange") companionChange: EventEmitter<string> = new EventEmitter<string>();

    // Selection
    @Input() selectedUser: UserModel;
    @Output("selectedUserChange") onselect: EventEmitter<UserModel> = new EventEmitter<UserModel>();

    constructor(
        private cdRef: ChangeDetectorRef,
        private usersStore: UsersStore,
        public userListService: UserListService,
        public listFiltersService: UserlistFiltersService){}

    ngOnInit() {
        this.filtersUpdatesSubscriber = this.listFiltersService.updateSubject.subscribe(() => this.cdRef.markForCheck());
        this.userUpdatesSubscriber = this.userListService.updateSubject.subscribe(() => this.cdRef.markForCheck());
        this.storeSubscriber = this.usersStore.onchange.subscribe((field) => {
            if (field == 'user') {
                this.cdRef.markForCheck();
            }
        });
    }

    ngOnDestroy() {
        this.filtersUpdatesSubscriber.unsubscribe();
        this.userUpdatesSubscriber.unsubscribe();
    }

    setStyles = (user: UserModel) => {
        return {
            selected: this.selectedUser && user && this.selectedUser.id === user.id
        }
    }
}