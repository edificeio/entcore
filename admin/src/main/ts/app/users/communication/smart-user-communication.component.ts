import { ActivatedRoute, Data, Router } from '@angular/router';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UserModel } from '../../core/store/models';
import { Subscription } from 'rxjs/Subscription';
import { SpinnerService } from '../../core/services';
import { CommunicationRulesService } from './communication-rules.service';
import { CommunicationRule } from './communication-rules.component';

@Component({
    selector: 'smart-user-communication',
    providers: [CommunicationRulesService],
    template: `
        <user-communication *ngIf="user && userSendingCommunicationRules" [user]="user"
                            [userSendingCommunicationRules]="userSendingCommunicationRules"
                            (close)="openUserDetails()"></user-communication>`
})
export class SmartUserCommunicationComponent implements OnInit, OnDestroy {

    public user: UserModel;
    public userSendingCommunicationRules: CommunicationRule[];

    private routeSubscription: Subscription;

    constructor(
        public spinner: SpinnerService,
        public communicationRulesService: CommunicationRulesService,
        private route: ActivatedRoute,
        private router: Router,
        private changeDetector: ChangeDetectorRef
    ) {
    }

    ngOnInit() {
        this.communicationRulesService.changes().subscribe(rules => {
            this.userSendingCommunicationRules = rules;
            this.changeDetector.markForCheck();
        });
        this.routeSubscription = this.route.data.subscribe((data: Data) => {
            this.user = data['user'];
            this.communicationRulesService.setGroups(data['groups']);
        });
    }

    public openUserDetails() {
        this.spinner.perform('portal-content', this.router.navigate([this.user.id, 'details'], {relativeTo: this.route.parent}));
    }

    ngOnDestroy(): void {
        this.routeSubscription.unsubscribe();
    }
}
