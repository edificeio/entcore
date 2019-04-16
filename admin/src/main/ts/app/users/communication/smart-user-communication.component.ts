import { ActivatedRoute, Data, Router } from '@angular/router';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UserModel, GroupModel } from '../../core/store/models';
import { Subscription } from 'rxjs/Subscription';
import { SpinnerService } from '../../core/services';
import { CommunicationRulesService } from './communication-rules.service';
import { CommunicationRule } from './communication-rules.component';
import { UsersStore } from '../users.store'

@Component({
    selector: 'smart-user-communication',
    providers: [CommunicationRulesService],
    template: `
        <user-communication *ngIf="user && userSendingCommunicationRules" 
                            [user]="user"
                            [userSendingCommunicationRules]="userSendingCommunicationRules"
                            [addCommunicationPickableGroups]="addCommunicationPickableGroups"
                            (close)="openUserDetails()">
        </user-communication>`
})
export class SmartUserCommunicationComponent implements OnInit, OnDestroy {

    public user: UserModel;
    public userSendingCommunicationRules: CommunicationRule[];
    public addCommunicationPickableGroups: GroupModel[];

    private communicationRulesChangesSubscription: Subscription;
    private routeSubscription: Subscription;

    constructor(
        public spinner: SpinnerService,
        public communicationRulesService: CommunicationRulesService,
        private route: ActivatedRoute,
        private router: Router,
        private changeDetector: ChangeDetectorRef,
        private usersStore: UsersStore
    ) {
    }

    ngOnInit() {
        this.communicationRulesChangesSubscription = this.communicationRulesService.changes().subscribe(rules => {
            this.userSendingCommunicationRules = rules;
            this.changeDetector.markForCheck();
        });
        this.routeSubscription = this.route.data.subscribe((data: Data) => {
            this.user = data['user'];
            this.communicationRulesService.setGroups(data['groups']);
        });
        this.addCommunicationPickableGroups = this.usersStore.structure.groups.data;
    }

    public openUserDetails() {
        this.spinner.perform('portal-content',
            this.router.navigate([this.user.id, 'details'],
                {
                    relativeTo: this.route.parent
                })
        );
    }

    ngOnDestroy(): void {
        this.communicationRulesChangesSubscription.unsubscribe();
        this.routeSubscription.unsubscribe();
    }
}
