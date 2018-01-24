import { Component, Input, Output, OnInit, OnDestroy, EventEmitter,
    ChangeDetectionStrategy, ChangeDetectorRef, AfterViewChecked } from '@angular/core'
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
        [isSelected]="isSelected"
        [limit]="userListService.limit"
        (inputChange)="userListService.inputFilter = $event"
        (onSelect)="selectedUser = $event; onselect.emit($event)"
        (scrolledDown)="userListService.addPageDown()"
        (listChange)="refreshListCount($event)">
        <div toolbar class="user-toolbar">
             <i class="fa is-size-5" aria-hidden="true"
                [ngClass]="{
                    'fa-sort-alpha-asc': userListService.sortsMap.alphabetical.sort === '+',
                    'fa-sort-alpha-desc': userListService.sortsMap.alphabetical.sort === '-',
                    'selected': userListService.sortsMap.alphabetical.selected
                }"
                [title]="'sort.alphabetical' | translate"
                (click)="userListService.changeSorts('alphabetical')"></i>
            <i class="fa is-size-5" aria-hidden="true"
                [ngClass]="{
                    'fa-sort-amount-asc': userListService.sortsMap.profile.sort === '+',
                    'fa-sort-amount-desc': userListService.sortsMap.profile.sort === '-',
                    'selected': userListService.sortsMap.profile.selected
                }"
                [title]="'sort.profile' | translate"
                (click)="userListService.changeSorts('profile')"></i>
            <strong class="badge">{{ nbUser }} <s5l>list.results.users</s5l></strong>
            <a class="button is-primary is-pulled-right" aria-hidden="true"
                [title]="'filters' | translate"
                [ngClass]="{'is-active': filtersOn()}"
                (click)="companionChange.emit('filter')">
                <s5l>filters</s5l>
                <i class="fa fa-chevron-right" aria-hidden="true"></i>
            </a>
        </div>

        <ng-template let-item>
            <span class="display-name">
                {{item.lastName?.toUpperCase()}} {{item.firstName}}
            </span>
            <span class="icons">
                <i class="fa fa-lock" 
                    *ngIf="item.code && item.code?.length > 0"
                    [title]="'user.icons.tooltip.inactive' | translate"></i>
                <i class="fa fa-ban" 
                    *ngIf="item.blocked"
                    [title]="'user.icons.tooltip.blocked' | translate"></i>
                <i class="fonticon duplicates" 
                    *ngIf="item.duplicates && item.duplicates?.length > 0"
                    [title]="'user.icons.tooltip.duplicated' | translate"></i>
                <i class="fa fa-times-circle" 
                    *ngIf="item.deleteDate"
                    [title]="'user.icons.tooltip.deleted' | translate"></i>
                <i class="fonticon waiting-predelete" 
                    *ngIf="!item.deleteDate && item.disappearanceDate"
                    [title]="'user.icons.tooltip.disappeared' | translate"></i>
            </span>
            <i class="profile" [ngClass]="item.type">{{item.type | translate}}</i>
        </ng-template>
    </list>
    `,
    styles: [`
        .user-toolbar {
            padding: 15px;
        }
        .user-toolbar i {
            cursor: pointer;
        }
    `],

    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserList implements OnInit, OnDestroy, AfterViewChecked {

    private filtersUpdatesSubscriber: Subscription;
    private userUpdatesSubscriber: Subscription;
    private storeSubscriber: Subscription;
    nbUser: number;

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
        return this.userlist.length != this.nbUser;
    }
}