import { Component, Input, ChangeDetectorRef } from '@angular/core'
import { Group, GroupUser } from '../../../../store'
import { ActivatedRoute, Router } from '@angular/router'
import { BundlesService } from 'sijil'
import { UserListService } from '../../../../services'

@Component({
    selector: 'group-users-list',
    template: `
        <list-component
            [model]="selectedGroup?.users"
            [inputFilter]="userListService.filterByInput"
            [sort]="userListService.sorts"
            searchPlaceholder="search.user"
            [display]="display"
            (inputChange)="userListService.inputFilter = $event"
            (onSelect)="selectUser($event)">
            <div toolbar>
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
                <ng-content></ng-content>
            </div>
        </list-component>
    ` ,
    styles: [``],
    providers: [ UserListService ]
})
export class GroupUsersList {

    constructor(
            private route: ActivatedRoute,
            private router: Router,
            private cdRef: ChangeDetectorRef,
            private bundles: BundlesService,
            private userListService: UserListService){}

    // Model
    @Input() selectedGroup : Group

    //protected isSelected = (user: GroupUser) => this.selectedUser === user
    protected selectUser(user: GroupUser) {
        if(user.structures.length > 0){
            this.router.navigate(['admin', user.structures[0].id, 'users', user.id])
        }
    }

    protected display = (user: GroupUser) => {
        let result : string = `${user.lastName} ${user.firstName} - ${this.bundles.translate(user.profile)}`
        if(user.structures.length === 1) {
            result += " - " + user.structures[0].name
        } else if (user.structures.length > 1) {
            result += " - " + this.bundles.translate('structure.or.more', {
                head: user.structures[0].name,
                rest: user.structures.length - 1
            })
        }
        return result
    }

}