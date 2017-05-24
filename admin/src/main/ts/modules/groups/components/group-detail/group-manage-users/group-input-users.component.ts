import { Component, Input, Output, ChangeDetectionStrategy, ChangeDetectorRef, DoCheck,  EventEmitter } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { UserListService } from '../../../../../services'
import { GroupsStore } from '../../../store'
import { UserModel } from '../../../../../store/models'

@Component({
    selector: 'group-input-users',
    template: `
        <div class="input-users">
            <select>
                <option>Test</option>
            </select>

            <list-component
                [model]="model"
                [filters]="filterUsers"
                [inputFilter]="userLS.filterByInput"
                [sort]="userLS.sorts"
                [display]="userLS.display"
                [ngClass]="setUserListStyles"
                (inputChange)="userLS.inputFilter = $event"
                (onSelect)="selectUser($event)">
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

                    <button class="select-all" (click)="selectAll()" *ngIf="!maxSelected"
                        [title]="'select.all' | translate">
                        <s5l>select.all</s5l>
                    </button>

                    <button class="deselect-all" (click)="deselectAll()"
                        [title]="'deselect.all' | translate">
                        <s5l>deselect.all</s5l>
                    </button>
                </div>
            </list-component>

            <div class="button-add">
                <button (click)="addUsers()"
                    [disabled]="selectedUsers.length === 0">+</button>
            </div>
        </div>
    `,
    providers: [ UserListService ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupInputUsers implements DoCheck {
    @Input() model: UserModel[] = []

    private selectedUsers: UserModel[] = []

    constructor(private groupsStore: GroupsStore,
        private userLS: UserListService,
        private cdRef: ChangeDetectorRef){}

    ngDoCheck(): void {
        this.cdRef.markForCheck()
    }

    private selectUser(u: UserModel): void {
        if (this.selectedUsers.indexOf(u) === -1) {
            this.selectedUsers.push(u)
        } else {
            this.selectedUsers = this.selectedUsers.filter(su => su.id !== u.id)
        }
    }

    private filterUsers = (u: {id: string, name: string}) => {
        return !this.groupsStore.group.users.find(ug => ug.id === u.id)
    }

    private setUserListStyles = (user: UserModel) => {
        return { selected: this.selectedUsers.indexOf(user) > -1 }
    }

    private selectAll(): void {
        this.selectedUsers = this.model.filter(u => {
            return this.groupsStore.group.users.map(gu => gu.id).indexOf(u.id) === -1
        })
    }

    private deselectAll(): void {
        this.selectedUsers = []
    }

    private addUsers(): void {
        this.groupsStore.group.addUsers(this.selectedUsers).then(() => {
            this.groupsStore.group.users = this.groupsStore.group.users.concat(this.selectedUsers)
            this.selectedUsers = []
            this.cdRef.markForCheck()
        })
    }
}
