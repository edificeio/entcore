import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { SpinnerService } from 'ngx-ode-ui';
import { routing } from 'src/app/core/services/routing.service';
import { GlobalStore } from 'src/app/core/store/global.store';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { UserModel } from 'src/app/core/store/models/user.model';
import { CommunicationRule } from '../../communication/communication-rules.component/communication-rules.component';
import { CommunicationRulesService } from '../../communication/communication-rules.service';
import { UsersStore } from '../users.store';

@Component({
    selector: 'ode-smart-user-communication',
    providers: [CommunicationRulesService],
    templateUrl: './smart-user-communication.component.html'
})
export class SmartUserCommunicationComponent extends OdeComponent implements OnInit, OnDestroy {

    public user: UserModel;
    public activeStructure: StructureModel;
    public manageableStructuresId: string[];
    public userSendingCommunicationRules: CommunicationRule[];
    public userReceivingCommunicationRules: CommunicationRule[];
    public addCommunicationPickableGroups: GroupModel[];


    constructor(
        injector: Injector,
        public spinner: SpinnerService,
        public communicationRulesService: CommunicationRulesService,
        private usersStore: UsersStore,
        public globalStore: GlobalStore
    ) {
        super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.communicationRulesService.changes()
            .subscribe(rules => {
                this.userSendingCommunicationRules = rules.sending;
                this.userReceivingCommunicationRules = rules.receiving;
                this.changeDetector.markForCheck();
            }));
        this.subscriptions.add(this.route.data.subscribe((data: Data) => {
            this.user = data.user;
            this.manageableStructuresId = this.globalStore.structures.data.map(s => s.id);
            this.communicationRulesService.setGroups(data.groups);
        }));
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

    public onGroupPickerStructureChange(structure: StructureModel) {
        structure.groups.sync().then(() => {
            this.addCommunicationPickableGroups = structure.groups.data;
            this.changeDetector.markForCheck();
        });
    }
}
