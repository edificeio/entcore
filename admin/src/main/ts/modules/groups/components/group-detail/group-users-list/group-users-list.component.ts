import { Component, Input, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core'
import { GroupModel, UserModel } from '../../../../../store'
import { ActivatedRoute, Router } from '@angular/router'
import { BundlesService } from 'sijil'
import { UserListService } from '../../../../../services'

@Component({
    selector: 'group-users-list',
    template: `
        <list-component
            [model]="selectedGroup?.users"
            [inputFilter]="userLS.filterByInput"
            [sort]="userLS.sorts"
            searchPlaceholder="search.user"
            [display]="display"
            (inputChange)="userLS.inputFilter = $event"
            (onSelect)="selectUser($event)">
            <div toolbar>
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
                <ng-content></ng-content>
            </div>
        </list-component>
    ` ,
    styles: [``],
    providers: [ UserListService ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupUsersList {

    constructor(
            private route: ActivatedRoute,
            private router: Router,
            private cdRef: ChangeDetectorRef,
            private bundles: BundlesService,
            private userLS: UserListService){}

    // Model
    @Input() selectedGroup : GroupModel

    protected selectUser(user: UserModel) {
        if(user.structures.length > 0){
            this.router.navigate(['admin', user.structures[0].id, 'users', user.id])
        }
    }

    protected display = (user: UserModel) => {
        let result : string = `${user.lastName} ${user.firstName} - ${this.bundles.translate(user.type)}`
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