import { ActivatedRoute, Data, Router } from '@angular/router';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { GroupModel, StructureModel } from '../../core/store/models';
import { routing, SpinnerService } from '../../core/services';
import { CommunicationRulesService } from '../../communication/communication-rules.service';
import { CommunicationRule } from '../../communication/communication-rules.component';
import { GlobalStore } from '../../core/store';
import { Subscription } from 'rxjs/Subscription';
import { GroupsStore } from '../groups.store';

@Component({
    selector: 'smart-group-communication',
    providers: [CommunicationRulesService],
    template: `
        <groups-communication *ngIf="!!group && !!groupsSendingCommunicationRules"
                              [title]="'group.communication.title' | translate:{name: group.name}"
                              [backButtonLabel]="'group.communication.back-to-group-details' | translate"
                              [sendingSectionTitle]="'group.communication.section.title.sending-rules'"
                              [receivingSectionTitle]="'group.communication.section.title.receiving-rules'"
                              [sendingSectionSendingColumnLabel]="'group.communication.current-group' | translate"
                              [sendingSectionReceivingColumnLabel]="'group.communication.groups-that-group-can-communicate-with' | translate"
                              [receivingSectionSendingColumnLabel]="'group.communication.groups-that-can-communicate-with-group' | translate"
                              [receivingSectionReceivingColumnLabel]="'group.communication.current-group' | translate"
                              [activeStructure]="activeStructure"
                              [manageableStructuresId]="manageableStructuresId"
                              [sendingCommunicationRules]="groupsSendingCommunicationRules"
                              [receivingCommunicationRules]="groupsReceivingCommunicationRules"
                              [addCommunicationPickableGroups]="addCommunicationPickableGroups"
                              [structures]="globalStore.structures.data"
                              (close)="openGroupDetails()"
                              (groupPickerStructureChange)="onGroupPickerStructureChange($event)">
        </groups-communication>`
})
export class SmartGroupCommunicationComponent implements OnInit, OnDestroy {

    public group: GroupModel;
    public activeStructure: StructureModel;
    public manageableStructuresId: string[];
    public groupsSendingCommunicationRules: CommunicationRule[];
    public groupsReceivingCommunicationRules: CommunicationRule[];
    public addCommunicationPickableGroups: GroupModel[];

    private communicationRulesChangesSubscription: Subscription;
    private routeSubscription: Subscription;
    private paramsSubscription: Subscription;

    constructor(
        public spinner: SpinnerService,
        public communicationRulesService: CommunicationRulesService,
        private route: ActivatedRoute,
        private router: Router,
        private changeDetector: ChangeDetectorRef,
        private groupsStore: GroupsStore,
        public globalStore: GlobalStore
    ) {
    }

    ngOnInit() {
        this.communicationRulesChangesSubscription = this.communicationRulesService.changes()
            .subscribe(rules => {
                this.groupsSendingCommunicationRules = rules.sending;
                this.groupsReceivingCommunicationRules = rules.receiving;
                this.changeDetector.markForCheck();
            });
        this.routeSubscription = this.route.data.subscribe((data: Data) => {
            this.group = data['group'];
            this.manageableStructuresId = this.globalStore.structures.data.map(s => s.id);
            this.communicationRulesService.setGroups([this.group]);
        });
        this.paramsSubscription = this.route.params
            .filter(params => params['groupId'])
            .subscribe(params =>
                this.groupsStore.group = this.groupsStore.structure.groups.data
                    .find(g => g.id === params['groupId'])
            );

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

    ngOnDestroy(): void {
        this.communicationRulesChangesSubscription.unsubscribe();
        this.routeSubscription.unsubscribe();
        this.paramsSubscription.unsubscribe();
    }

    public onGroupPickerStructureChange(structure: StructureModel) {
        structure.groups.sync().then(() => {
            this.addCommunicationPickableGroups = structure.groups.data;
            this.changeDetector.markForCheck();
        });
    }
}
