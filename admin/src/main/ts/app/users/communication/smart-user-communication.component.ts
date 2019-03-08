import { ActivatedRoute, Data, Router } from '@angular/router';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { UserDetailsModel } from '../../core/store/models';
import { Subscription } from 'rxjs/Subscription';
import { SpinnerService } from '../../core/services';
import { UsersStore } from '../users.store';

@Component({
    selector: 'smart-user-communication',
    template: `
        <user-communication *ngIf="user" [user]="user" (close)="openUserDetails()"></user-communication>`
})
export class SmartUserCommunicationComponent implements OnInit, OnDestroy {
    user: UserDetailsModel;

    private usersStoreChangeSubscription: Subscription;
    private routeSubscription: Subscription;

    constructor(
        public spinner: SpinnerService,
        private usersStore: UsersStore,
        private route: ActivatedRoute,
        private router: Router
    ) {
    }

    ngOnInit() {
        this.usersStoreChangeSubscription = this.usersStore.onchange.subscribe(field => {
            if (field === 'user') {
                if (this.usersStore.user &&
                    !this.usersStore.user.structures.find(
                        s => this.usersStore.structure.id === s.id)) {
                    setTimeout(() => {
                        this.router.navigate(['..'],
                            {relativeTo: this.route, replaceUrl: true});
                    }, 0);
                } else {
                    this.user = this.usersStore.user.userDetails;
                }
            }
        });
        this.routeSubscription = this.route.data.subscribe((data: Data) => {
            this.usersStore.user = data['user'];
        });
    }

    ngOnDestroy(): void {
        this.usersStoreChangeSubscription.unsubscribe();
        this.routeSubscription.unsubscribe();
    }

    openUserDetails() {
        this.spinner.perform('portal-content', this.router.navigate([this.user.id, 'details'], {relativeTo: this.route.parent}));
    }
}
