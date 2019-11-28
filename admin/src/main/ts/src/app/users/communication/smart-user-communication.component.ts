import {ActivatedRoute, Data, Router} from '@angular/router';
import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {CommunicationRulesService} from '../../communication/communication-rules.service';
import {CommunicationRule} from '../../communication/communication-rules.component/communication-rules.component';
import {UsersStore} from '../users.store';
import {Subscription} from 'rxjs';
import { UserModel } from 'src/app/core/store/models/user.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { GlobalStore } from 'src/app/core/store/global.store';
import { SpinnerService } from 'src/app/core/services/spinner.service';
import { routing } from 'src/app/core/services/routing.service';

@Component({
    selector: 'ode-smart-user-communication',
    providers: [CommunicationRulesService],
    templateUrl: './smart-users-comparison.component.html'
})
export class SmartUserCommunicationComponent implements OnInit, OnDestroy {

    public user: UserModel;
    public activeStructure: StructureModel;
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
        public globalStore: GlobalStore
    ) {
    }

    ngOnInit() {
        this.communicationRulesChangesSubscription = this.communicationRulesService.changes()
            .subscribe(rules => {
                this.userSendingCommunicationRules = rules.sending;
                this.userReceivingCommunicationRules = rules.receiving;
                this.changeDetector.markForCheck();
            });
        this.routeSubscription = this.route.data.subscribe((data: Data) => {
            this.user = data.user;
            this.manageableStructuresId = this.globalStore.structures.data.map(s => s.id);
            this.communicationRulesService.setGroups(data.groups);
        });
        this.addCommunicationPickableGroups = this.usersStore.structure.groups.data;
        const activeStructureId = routing.getParam(this.route.snapshot, 'structureId');
        this.activeStructure = this.globalStore.structures.data.find(s => s.id === activeStructureId);
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
            this.addCommunicationPickableGroups = structure.groups.data;
            this.changeDetector.markForCheck();
        });
    }
}
