import { Component, Input, Output, ChangeDetectionStrategy, ChangeDetectorRef, EventEmitter } from '@angular/core'

import { GroupsStore } from '../../../../store'
import { UserListService, LoadingService, NotifyService } from '../../../../../../services'
import { UserModel } from '../../../../../../store/models'

@Component({
    selector: 'group-output-users',
    template: `
        <div class="header">
            <s5l>group.manage.users.added</s5l>
        </div>

        <div class="flex-row-wrap">
            <button (click)="removeUsers()"
                [disabled]="selectedUsers.length === 0"
                class="remove"
                [title]="'group.manage.users.button.remove' | translate">-</button>

            <list-component
                [model]="model"
                [sort]="userLS.sorts"
                [inputFilter]="userLS.filterByInput"
                [ngClass]="setUserListStyles"
                (inputChange)="userLS.inputFilter = $event"
                (onSelect)="selectUser($event)"
                (listChange)="storedElements = $event"
                noResultsLabel="list.results.no.users">
                <div toolbar class="user-toolbar">
                    <i class="fa" aria-hidden="true"
                        [ngClass]="{
                            'fa-sort-alpha-asc': userLS.sortsMap.alphabetical.sort === '+',
                            'fa-sort-alpha-desc': userLS.sortsMap.alphabetical.sort === '-',
                            'selected': userLS.sortsMap.alphabetical.selected
                        }"
                        [tooltip]="'sort.alphabetical' | translate" position="top"
                        (click)="userLS.changeSorts('alphabetical')"></i>

                    <i class="fa" aria-hidden="true"
                        [ngClass]="{
                            'fa-sort-amount-asc': userLS.sortsMap.profile.sort === '+',
                            'fa-sort-amount-desc': userLS.sortsMap.profile.sort === '-',
                            'selected': userLS.sortsMap.profile.selected
                        }"
                        [tooltip]="'sort.profile' | translate" position="top"
                        (click)="userLS.changeSorts('profile')"></i>

                    <button class="select-all" (click)="selectAll()"
                        [title]="'select.all' | translate">
                        <s5l>select.all</s5l>
                    </button>

                    <button class="deselect-all" (click)="deselectAll()"
                        [title]="'deselect.all' | translate">
                        <s5l>deselect.all</s5l>
                    </button>
                </div>

                <ng-template let-item>
                    <span class="display-name">
                        {{item?.lastName.toUpperCase()}} {{item?.firstName}}
                    </span>
                    <i class="profile" [ngClass]="item.type">{{item.type | translate}}</i>
                </ng-template>
            </list-component>
        </div>
    `,
    providers: [ UserListService ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupOutputUsers {
    @Input() model: UserModel[] = []
    @Output() onDelete: EventEmitter<any> = new EventEmitter()

    // list elements stored by store pipe in list-component 
    // (takes filters in consideration)
    private storedElements: UserModel[] = []

    // Users selected by enduser
    private selectedUsers: UserModel[] = []

    constructor(private groupsStore: GroupsStore,
        private cdRef: ChangeDetectorRef,
        private userLS: UserListService,
        private ls: LoadingService,
        private ns: NotifyService){}

    private selectUser(u): void {
        if (this.selectedUsers.indexOf(u) === -1) {
            this.selectedUsers.push(u)
        } else {
            this.selectedUsers = this.selectedUsers.filter(su => su.id !== u.id)
        }
    }

    private setUserListStyles = (user: UserModel) => {
        return { selected: this.selectedUsers.indexOf(user) > -1 }
    }

    private selectAll(): void {
        this.selectedUsers = this.storedElements
    }

    private deselectAll(): void {
        this.selectedUsers = []
    }

    private removeUsers(): void {
        this.ls.perform('group-manage-users',
            this.groupsStore.group.removeUsers(this.selectedUsers)
                .then(() => {
                    this.groupsStore.group.users = this.groupsStore.group.users
                        .filter(gu => this.selectedUsers.indexOf(gu) === -1)
                    this.onDelete.emit()
                    this.selectedUsers = []
                    this.ns.success('notify.group.manage.users.removed.content')
                    this.cdRef.markForCheck()
                })
                .catch((err) => {
                    this.ns.error('notify.group.manage.users.removed.error.content', 'notify.group.manage.users.removed.error.title', err)
                })
        )
    }
}
