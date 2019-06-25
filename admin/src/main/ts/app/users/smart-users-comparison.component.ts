import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { UserOverview } from './user-overview.component';
import { UserService } from './user.service';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/switchMap';
import 'rxjs/add/operator/do';

interface Users<K> {
    user1: K;
    user2: K;
}

@Component({
    selector: 'smart-users-comparison',
    template: `
        <users-comparison *ngIf="user1overview && user2overview; else wait"
                          [user1]="user1overview"
                          [user2]="user2overview"></users-comparison>
        <ng-template #wait>...</ng-template>
    `
})
export class SmartUsersComparisonComponent implements OnInit, OnChanges {
    @Input()
    public user1: string;

    @Input()
    public user2: string;

    public user1overview: UserOverview;
    public user2overview: UserOverview;

    private usersChanged: Subject<Users<string>> = new Subject();

    constructor(private userService: UserService) {
        this.usersChanged.asObservable()
            .do(() => {
                this.user1overview = null;
                this.user2overview = null;
            })
            .switchMap(users => this.fetchUsers(users))
            .subscribe(users => {
                this.user1overview = users.user1;
                this.user2overview = users.user2;
            });
    }

    ngOnInit(): void {
        this.usersChanged.next({user1: this.user1, user2: this.user2});
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.usersChanged.next({user1: this.user1, user2: this.user2});
    }

    private fetchUsers(users: Users<string>): Observable<Users<UserOverview>> {
        return Observable
            .forkJoin(this.userService.fetch(users.user1), this.userService.fetch(users.user2))
            .map(([user1, user2]) => ({user1, user2}));
    }
}
