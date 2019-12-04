import { OdeComponent } from './../../../core/ode/OdeComponent';
import { Data } from '@angular/router';
import { Component, OnDestroy, OnInit, Injector, Type } from '@angular/core';
import { CommunicationRulesService } from '../../../communication/communication-rules.service';
import { CommunicationRule } from '../../../communication/communication-rules.component/communication-rules.component';
import { GroupsStore } from '../../groups.store';
import { filter } from 'rxjs/operators';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { SpinnerService } from 'ngx-ode-ui';
import { GlobalStore } from 'src/app/core/store/global.store';
import { routing } from 'src/app/core/services/routing.service';

@Component({
    selector: 'ode-smart-group-communication',
    providers: [CommunicationRulesService],
    templateUrl: './smart-group-communication.component.html'
})
export class SmartGroupCommunicationComponent extends OdeComponent implements OnInit, OnDestroy {

    public group: GroupModel;
    public activeStructure: StructureModel;
    public manageableStructuresId: string[];
    public groupsSendingCommunicationRules: CommunicationRule[];
    public groupsReceivingCommunicationRules: CommunicationRule[];
    public addCommunicationPickableGroups: GroupModel[];

    public spinner: SpinnerService;
    public communicationRulesService: CommunicationRulesService;
    private groupsStore: GroupsStore;
    public globalStore: GlobalStore;

    constructor(injector: Injector) {
        super(injector);
        this.spinner = injector.get<SpinnerService>(SpinnerService as Type<SpinnerService>);
        this.communicationRulesService = injector.get<CommunicationRulesService>(CommunicationRulesService as Type<CommunicationRulesService>);
        this.groupsStore = injector.get<GroupsStore>(GroupsStore as Type<GroupsStore>);
        this.globalStore = injector.get<GlobalStore>(GlobalStore as Type<GlobalStore>);
    }

    ngOnInit() {
        this.subscriptions.add(this.communicationRulesService.changes()
            .subscribe(rules => {
                this.groupsSendingCommunicationRules = rules.sending;
                this.groupsReceivingCommunicationRules = rules.receiving;
                this.changeDetector.markForCheck();
            }));
        this.subscriptions.add(this.route.data.subscribe((data: Data) => {
            this.group = data.group;
            this.manageableStructuresId = this.globalStore.structures.data.map(s => s.id);
            this.communicationRulesService.setGroups([this.group]);
        }));
        this.subscriptions.add(this.route.params
            .pipe(filter(params => params.groupId))
            .subscribe(params =>
                this.groupsStore.group = this.groupsStore.structure.groups.data
                    .find(g => g.id === params.groupId)
            ));

        const activeStructureId = routing.getParam(this.route.snapshot, 'structureId');
        this.activeStructure = this.globalStore.structures.data.find(s => s.id === activeStructureId);
        this.addCommunicationPickableGroups = this.activeStructure.groups.data;
    }

    public openGroupDetails() {
        this.spinner.perform('portal-content',
            this.router.navigate([this.group.id, 'details'],
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
