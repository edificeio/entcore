import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Router } from '@angular/router';

import { UserModel } from '../../../core/store';
import { UserListService } from '../../../core/services';

@Component({
    selector: 'group-users-list',
    template: `
        <list [model]="users"
              [inputFilter]="userLS.filterByInput"
              [sort]="userLS.sorts"
              searchPlaceholder="search.user"
              (inputChange)="userLS.inputFilter = $event"
              (onSelect)="selectUser($event)"
              noResultsLabel="list.results.no.users">
            <div class="tools" toolbar>
                <i class="tools__tool fa" aria-hidden="true"
                   [ngClass]="{
                    'fa-sort-alpha-asc': userLS.sortsMap.alphabetical.sort === '+',
                    'fa-sort-alpha-desc': userLS.sortsMap.alphabetical.sort === '-',
                    'selected': userLS.sortsMap.alphabetical.selected
                }"
                   [tooltip]="'sort.alphabetical' | translate" position="top"
                   (click)="userLS.changeSorts('alphabetical')"></i>
                <i class="tools__tool fa" aria-hidden="true"
                   [ngClass]="{
                        'fa-sort-amount-asc': userLS.sortsMap.profile.sort === '+',
                        'fa-sort-amount-desc': userLS.sortsMap.profile.sort === '-',
                        'selected': userLS.sortsMap.profile.selected
                    }"
                   [tooltip]="'sort.profile' | translate" position="top"
                   (click)="userLS.changeSorts('profile')"></i>
                <strong class="tools__tool badge">
                    {{ users.length }}
                    {{ 'members' | translate:{count: users.length} | lowercase }}
                </strong>
                <span class="tools__tool tools__tool--injected"><ng-content></ng-content></span>
            </div>

            <ng-template let-item>
                <span class="display-name">{{item?.lastName.toUpperCase()}} {{item?.firstName}}</span>

                <i class="profile" [ngClass]="item.type">{{item.type | translate}}</i>
                <span class="structures">
                    <ul>
                        <li *ngFor="let s of item?.structures">{{ s.name }}</li>
                    </ul>
                </span>
            </ng-template>
        </list>
    `,
    styles: ['.tools {display: flex;}', '.tools__tool {flex-grow: 0; flex-shrink: 0; padding: 0 5px;}', '.tools__tool--injected {text-align: right; flex-grow: 1;}'],
    providers: [UserListService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupUsersList {
    constructor(private router: Router, public userLS: UserListService) {
    }

    @Input()
    users: UserModel[];

    selectUser(user: UserModel) {
        if (user.structures.length > 0) {
            this.router.navigate(['admin', user.structures[0].id, 'users', user.id]);
        }
    }
}
