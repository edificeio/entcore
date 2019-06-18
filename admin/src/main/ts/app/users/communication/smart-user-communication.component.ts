import { ActivatedRoute, Data, Router } from '@angular/router';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { GroupModel, UserModel, StructureModel } from '../../core/store/models';
import { routing, SpinnerService } from '../../core/services';
import { CommunicationRulesService } from './communication-rules.service';
import { CommunicationRule } from './communication-rules.component';
import { UsersStore } from '../users.store';
import { GlobalStore, globalStore } from '../../core/store';
import { Subscription } from 'rxjs/Subscription';

@Component({
    selector: 'smart-user-communication',
    providers: [CommunicationRulesService],
    template: `
        <user-communication *ngIf="user && userSendingCommunicationRules"
                            [user]="user"
                            [activeStructureId]="activeStructureId"
                            [manageableStructuresId]="manageableStructuresId"
                            [userSendingCommunicationRules]="userSendingCommunicationRules"
                            [userReceivingCommunicationRules]="userReceivingCommunicationRules"
                            [addCommunicationPickableGroups]="addCommunicationPickableGroups"
                            [structures]="globalStore.structures.data"
                            [structure]="structure"
                            (close)="openUserDetails()"
                            (groupPickerStructureChange)="onGroupPickerStructureChange($event)">
        </user-communication>`
})
export class SmartUserCommunicationComponent implements OnInit, OnDestroy {

    public user: UserModel;
    public activeStructureId: string;
    public structure: StructureModel;
    public manageableStructuresId: string[];
    public userSendingCommunicationRules: CommunicationRule[];
    public userReceivingCommunicationRules: CommunicationRule[];
    public addCommunicationPickableGroups: GroupModel[];

    private communicationRulesChangesSubscription: Subscription;
    private routeSubscription: Subscription;

    constructor(
        public spinner: SpinnerService,
        public communicationRulesService: CommunicationRulesService,
        private route: ActivatedRoute,
        private router: Router,
        private changeDetector: ChangeDetectorRef,
        private usersStore: UsersStore,
        private globalStore: GlobalStore
    ) {
    }

    ngOnInit() {
        this.communicationRulesChangesSubscription = this.communicationRulesService.changes().subscribe(rules => {
            this.userSendingCommunicationRules = rules.sending;
            this.userReceivingCommunicationRules = rules.receiving;
            this.changeDetector.markForCheck();
        });
        this.routeSubscription = this.route.data.subscribe((data: Data) => {
            this.user = data['user'];
            this.manageableStructuresId = this.globalStore.structures.data.map(s => s.id);
            this.communicationRulesService.setGroups(data['groups']);
        });
        this.addCommunicationPickableGroups = this.usersStore.structure.groups.data;
        this.activeStructureId = routing.getParam(this.route.snapshot, 'structureId');
        this.structure = globalStore.structures.data.find(s => s.id === this.activeStructureId);
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

    public onGroupPickerStructureChange(structure: StructureModel) {
        structure.groups.sync().then(() => {
            this.addCommunicationPickableGroups = structure.groups.data
            this.changeDetector.markForCheck();
        });
    }
}
