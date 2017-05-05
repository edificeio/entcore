import { Component, Input, Output, OnInit, EventEmitter,
    ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core'
import { BundlesService } from 'sijil'

import { User } from '../../../models/mappings'
import { UserListService } from '../../../services'
import { UserlistFiltersService } from '../../../services/users/userlist.filters.service'
import { OnDestroy } from '@angular/core'
import { Subscription } from 'rxjs'

@Component({
    selector: 'user-list',
    template: `
    <list-component
        [model]="userlist"
        [filters]="listFilters.getFormattedFilters()"
        [inputFilter]="userListService.filterByInput"
        [sort]="userListService.sorts"
        [limit]="userListService.limit"
        searchPlaceholder="search.user"
        [ngClass]="setStyles"
        [display]="userListService.display"
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
        </div>
    </list-component>
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
    host: {
        '(document:scroll)': 'onDocumentScroll($event)',
    },
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [ UserListService ]
})
export class UserList implements OnInit, OnDestroy {

    constructor(private cdRef: ChangeDetectorRef,
        private bundlesService: BundlesService,
        public userListService: UserListService,
        private listFilters: UserlistFiltersService){}

    ngOnInit() {
        this.filtersUpdatesSubscriber = this.listFilters.updateSubject.subscribe(() => {
            this.cdRef.markForCheck()
        })
    }

    ngOnDestroy() {
        this.filtersUpdatesSubscriber.unsubscribe()
    }

    private filtersUpdatesSubscriber: Subscription

    @Input() userlist: User[] = []

    @Input() listCompanion: string
    @Output("listCompanionChange") companionChange: EventEmitter<string> = new EventEmitter<string>()

    // Selection
    @Input() selectedUser: User
    @Output("selectedUserChange") onselect: EventEmitter<User> = new EventEmitter<User>()

    private selectUser(user: User) {
        this.selectedUser = user
        this.onselect.emit(user)
    }

    private setStyles = (user: User) => {
        return {
            selected: this.isSelected(user),
            blocked: user.blocked,
            duplicates: user.duplicates && user.duplicates.length > 0
        }
    }

    private isSelected = (user: User) => {
        return this.selectedUser && user &&
            this.selectedUser.id === user.id
    }

    // Scroll
    private ticking = false;
    private onDocumentScroll(event) {
        if (!this.ticking) {
            window.requestAnimationFrame(() => {
                 if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight) {
                    this.userListService.addPage(this.userlist.length)
                    this.cdRef.markForCheck()
                }
                this.ticking = false;
            });
        }
        this.ticking = true;
    }

}