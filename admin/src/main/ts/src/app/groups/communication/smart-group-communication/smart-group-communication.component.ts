import {ActivatedRoute, Data, Router} from '@angular/router';
import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {CommunicationRulesService} from '../../../communication/communication-rules.service';
import {CommunicationRule} from '../../../communication/communication-rules.component/communication-rules.component';
import {Subscription} from 'rxjs';
import {GroupsStore} from '../../groups.store';
import {filter} from 'rxjs/operators';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { SpinnerService } from 'src/app/core/services/spinner.service';
import { GlobalStore } from 'src/app/core/store/global.store';
import { routing } from 'src/app/core/services/routing.service';

@Component({
    selector: 'ode-smart-group-communication',
    providers: [CommunicationRulesService],
    templateUrl: './smart-group-communication.component.html'
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
            this.group = data.group;
            this.manageableStructuresId = this.globalStore.structures.data.map(s => s.id);
            this.communicationRulesService.setGroups([this.group]);
        });
        this.paramsSubscription = this.route.params
            .pipe(filter(params => params.groupId))
            .subscribe(params =>
                this.groupsStore.group = this.groupsStore.structure.groups.data
                    .find(g => g.id === params.groupId)
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
