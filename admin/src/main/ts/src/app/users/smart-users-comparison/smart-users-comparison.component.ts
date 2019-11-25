import {ChangeDetectorRef, Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {UserOverview} from '../user-overview/user-overview.component';
import {UserService} from '../user.service';
import {forkJoin, Observable, Subject} from 'rxjs';
import {map, switchMap, tap} from 'rxjs/operators';


interface Users<K> {
    user1: K;
    user2: K;
}

@Component({
    selector: 'ode-smart-users-comparison',
    templateUrl: './smart-users-comparison.component.html'
})
export class SmartUsersComparisonComponent implements OnInit, OnChanges {
    @Input()
    public user1: string;

    @Input()
    public user2: string;

    public user1overview: UserOverview;
    public user2overview: UserOverview;

    private usersChanged: Subject<Users<string>> = new Subject();

    constructor(
        private userService: UserService,
        private changeDetectorRef: ChangeDetectorRef
    ) {
        this.usersChanged.asObservable()
            .pipe(
                tap(() => {
                    this.user1overview = null;
                    this.user2overview = null;
                }),
                switchMap(users => this.fetchUsers(users))
            )
            .subscribe(users => {
                this.user1overview = users.user1;
                this.user2overview = users.user2;
                this.changeDetectorRef.markForCheck();
            });
    }

    ngOnInit(): void {
        this.usersChanged.next({user1: this.user1, user2: this.user2});
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.usersChanged.next({user1: this.user1, user2: this.user2});
    }

    private fetchUsers(users: Users<string>): Observable<Users<UserOverview>> {
        return forkJoin([this.userService.fetch(users.user1), this.userService.fetch(users.user2)])
            .pipe(
                map(([user1, user2]) => ({user1, user2}))
            );
    }
}
