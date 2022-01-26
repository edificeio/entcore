import { Component, Injector, OnDestroy, OnInit, Type } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { SpinnerService } from 'ngx-ode-ui';
import { filter } from 'rxjs/operators';
import { routing } from 'src/app/core/services/routing.service';
import { GlobalStore } from 'src/app/core/store/global.store';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { CommunicationRule } from '../../../communication/communication-rules.component/communication-rules.component';
import { CommunicationRulesService } from '../../../communication/communication-rules.service';
import { GroupsStore } from '../../groups.store';
import {ReplaySubject} from 'rxjs';

@Component({
    selector: 'ode-smart-group-communication',
    providers: [CommunicationRulesService],
    templateUrl: './smart-group-communication.component.html'
})
export class SmartGroupCommunicationComponent extends OdeComponent implements OnInit, OnDestroy {

    public group: GroupModel;
    public activeStructure: StructureModel;
    public manageableStructuresId: string[];
    public groupsSendingCommunicationRules = new ReplaySubject<CommunicationRule[]>();
    public groupsReceivingCommunicationRules = new ReplaySubject<CommunicationRule[]>();
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
                this.groupsSendingCommunicationRules.next(rules.sending);
                this.groupsReceivingCommunicationRules.next(rules.receiving);
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

    public get titleLabel() {
        return this.group.subType==='BroadcastGroup' ? 'broadcastlist.communication.title' : 'group.communication.title';
    }
    public get backButtonLabel() {
        return this.group.subType==='BroadcastGroup' ? 'broadcastlist.communication.back-to-list-details' : 'group.communication.back-to-group-details';
    }
    public get sendingSectionTitle() {
        return this.group.subType==='BroadcastGroup' ? '' : 'group.communication.section.title.sending-rules'; // empty string will hide the entire section
    }
    public get receivingSectionTitle() {
        return this.group.subType==='BroadcastGroup' ? 'broadcastlist.communication.section.title.receiving-rules' : 'group.communication.section.title.receiving-rules';
    }
    public get receivingSectionSendingColumnLabel() {
        return this.group.subType==='BroadcastGroup' ? 'broadcastlist.communication.groups-that-can-communicate-with-list' : 'group.communication.groups-that-can-communicate-with-group';
    }
    public get receivingSectionReceivingColumnLabel() {
        return this.group.subType==='BroadcastGroup' ? 'broadcastlist.communication.current-list' : 'group.communication.current-group';
    }
}
