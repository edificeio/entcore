import {ChangeDetectorRef, Component, EventEmitter, Injector, Input, OnInit, Output} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {BundlesService} from 'ngx-ode-sijil';
import {CommunicationRulesService} from '../communication-rules.service';
import {combineLatest, Observable, ReplaySubject, Subject} from 'rxjs';
import { standardise } from 'ngx-ode-ui';

import {catchError, filter, first, map, mergeMap, tap} from 'rxjs/operators';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { NotifyService } from 'src/app/core/services/notify.service';
import { GroupNameService } from 'src/app/core/services/group-name.service';
import {OdeComponent} from 'ngx-ode-core';

const css = {
    group: 'lct-user-communication-group',
    sendingColumn: 'lct-user-communication-sending-column',
    receivingColumn: 'lct-user-communication-receiving-column'
};

export const communicationRulesLocators = {
    group: `.${css.group}`,
    sendingColumn: `.${css.sendingColumn}`,
    receivingColumn: `.${css.receivingColumn}`
};

const WARNING_ENDGROUP_USERS_CAN_COMMUNICATE = 'endgroup-users-can-communicate';
const WARNING_STARTGROUP_USERS_CAN_COMMUNICATE = 'startgroup-users-can-communicate';
const WARNING_BOTH_GROUPS_USERS_CAN_COMMUNICATE = 'both-groups-users-can-communicate';

@Component({
    selector: 'ode-communication-rules',
    templateUrl: './communication-rules.component.html',
    styleUrls: ['./communication-rules.component.scss']
})
export class CommunicationRulesComponent  extends OdeComponent implements OnInit {
    get activeStructure(): StructureModel {
        return this._activeStructure;
    }
    @Input()
    set activeStructure(value: StructureModel) {
        this._activeStructure = value;
        this.$activeStructure.next(value);
    }
    get communicationRules(): CommunicationRule[] {
        return this._communicationRules;
    }

    @Input()
    set communicationRules(value: CommunicationRule[]) {
        this._communicationRules = value;
        if (value) {
            this.getSenders();
            this.getReceivers();
        }
    }
    @Input()
    public sendingHeaderLabel = '';

    @Input()
    public receivingHeaderLabel = '';

    private _communicationRules: CommunicationRule[];

    @Input()
    public addCommunicationPickableGroups: GroupModel[];

    @Input()
    public activeColumn: Column;


    private _activeStructure: StructureModel;

    @Input()
    public manageableStructuresId: string[];

    @Input()
    public structures: StructureModel[];

    @Output()
    public groupPickerStructureChange: EventEmitter<StructureModel> = new EventEmitter<StructureModel>();

    public selected: Cell;
    public highlighted: Cell;

    public removeConfirmationDisplayed = false;
    public $removeConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();
    public addConfirmationDisplayed = false;
    public $addConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public showGroupPicker = false;
    public warningGroupSender = false;
    public warningGroupReceiver = false;
    public pickedGroup: GroupModel;
    public $receiversCommunicationRules = new ReplaySubject<GroupModel[]>();
    public $sendersCommunicationRules = new ReplaySubject<GroupModel[]>();

    public $receivers: Observable<GroupModel[]>;
    public $senders: Observable<GroupModel[]>;

    public $activeStructure = new ReplaySubject<StructureModel>();
    public filterSenders: string[];
    public filterReceivers: string[];

    constructor(private communicationRulesService: CommunicationRulesService,
                private notifyService: NotifyService,
                public groupNameService: GroupNameService,
                private bundlesService: BundlesService,
                private changeDetectorRef: ChangeDetectorRef,
                injector: Injector) {
        super(injector);
    }

    public ngOnInit(): void {
        super.ngOnInit();

        this.$senders = combineLatest([this.$activeStructure, this.$sendersCommunicationRules]).pipe(
            map((result: [StructureModel, GroupModel[]]) => sortGroups(uniqueGroups(result[1]), (g) => this.groupNameService.getGroupName(g), result[0].id)));

        this.$receivers = combineLatest([this.$activeStructure, this.$receiversCommunicationRules]).pipe(
            map((result: [StructureModel, GroupModel[]]) => sortGroups(uniqueGroups(result[1]), (g) => this.groupNameService.getGroupName(g), result[0].id))
        );

    }

    public select(column: Column, group: GroupModel): void {
        this.selected = {column, group};
        this.resetHighlight();
    }

    public highlight(column: Column, group: GroupModel, selected: Cell): void {
        if (!selected || column !== selected.column || group.id !== selected.group.id) {
            this.highlighted = {column, group};
        }
    }

    public resetHighlight(): void {
        this.highlighted = null;
    }

    public toFilterWords(filter: string): string[] {
        return filter === null || filter === '' ? null : standardise(filter).split(/\s+/);
    }

    public filterRule(sender: GroupModel, receivers: GroupModel[]): boolean
    {
        const senderWords = this.filterSenders;
        const receiveWords = this.filterReceivers;
        let included = null;

        if (senderWords != null)
        {
            if(sender != null)
            {
                const name = standardise(this.groupNameService.getGroupName(sender));
                for (let i = senderWords.length; i-- > 0;) {
                    if (name.indexOf(senderWords[i]) === -1) {
                        included = false;
                        break;
                    }
                }
            }
            else
                included = false;
        }

        if (included == null && receiveWords != null) {
            included = false;
            loopReceivers:
                for (let r = receivers.length; r-- > 0;) {
                    const rName = standardise(this.groupNameService.getGroupName(receivers[r]));
                    for (let i = receiveWords.length; i-- > 0;) {
                        if (rName.indexOf(receiveWords[i]) !== -1) {
                            included = true;
                            break loopReceivers;
                        }
                    }
                }
        }

        return included == null ? true : included;
    }

    public getSenders(): void {
        const senders = this._communicationRules
            .filter(rule => this.filterRule(rule.sender, rule.receivers))
            .map(rule => rule.sender)
            .filter(sender => sender != null);
        this.$sendersCommunicationRules.next(senders);
    }

    public getReceivers(): void {
        const receivers: GroupModel[] = [];
        this._communicationRules.forEach(rule => {
            for (let i = rule.receivers.length; i-- > 0;) {
                if (this.filterRule(rule.sender, [rule.receivers[i]]) === true) {
                    receivers.push(rule.receivers[i]);
                }
            }
        });
        this.$receiversCommunicationRules.next(receivers);
    }

    public trackByGroupId(index: number, group: GroupModel): string {
        return group.id;
    }

    public isSelected(column: Column, group: GroupModel, selected: Cell) {
        if (!selected) {
            return false;
        }

        return group.id === selected.group.id && column === selected.column;
    }

    public isRelatedWithCell(column: Column, group: GroupModel, cell: Cell, communicationRules: CommunicationRule[]): boolean {
        if (!cell) {
            return false;
        }

        if (column === cell.column) {
            return group.id === cell.group.id;
        }

        return communicationRules
            .filter(cr => cell.column === 'sending' ? (cr.sender ? (cr.sender.id === cell.group.id) : false) : cr.receivers.some(g => g.id === cell.group.id))
            .some(cr => cell.column === 'sending' ? cr.receivers.some(g => g.id === group.id) : (cr.sender ? (cr.sender.id === group.id) : false));
    }

    public removeCommunication(sender: GroupModel, receiver: GroupModel) {
        this.removeConfirmationDisplayed = true;
        this.$removeConfirmationClicked.asObservable()
            .pipe(  first(),
                tap(() => this.removeConfirmationDisplayed = false),
                filter(choice => choice === 'confirm'),
                mergeMap(() => this.communicationRulesService.removeCommunication(sender, receiver, this.activeColumn))
            )
            .subscribe(() => this.notifyService.success({
                key: 'user.communication.remove-communication.success',
                parameters: {groupName: this.groupNameService.getGroupName(sender)}
            }), () => this.notifyService.error({
                key: 'user.communication.remove-communication.error.content',
                parameters: {groupName: this.groupNameService.getGroupName(sender)}
            }, 'user.communication.remove-communication.error.title'));
    }

    public filterGroupPicker = (group: GroupModel) => {
        if (this.selected) {
            if (group.id === this.selected.group.id) {
                return false;
            }

            if (this.activeColumn === 'sending') {
                return !this._communicationRules
                    .find(commRule => commRule.sender.id === this.selected.group.id).receivers
                    .find(receiver => receiver.id === group.id);
            } else {
                return !this._communicationRules
                    .filter(commRule => commRule.receivers.some(receiver => receiver.id === this.selected.group.id))
                    .some(commRule => !!commRule.sender && (commRule.sender.id === group.id));
            }
        }
        return true;
    }

    public isGroupInAManageableStructure(g: GroupModel, structuresId: string[]): boolean {
        return structuresId.indexOf(getStructureIdOfGroup(g)) > -1;
    }

    public isCommunicationRuleManageable(group: GroupModel, cell: Cell, structuresId: string[]): boolean {
        if (!group || !cell) {
            return false;
        }

        return this.isGroupInAManageableStructure(group, structuresId)
            && this.isGroupInAManageableStructure(cell.group, structuresId);
    }

    public onGroupPick(pickedGroup: GroupModel) {
        const sender: GroupModel = this.activeColumn === 'sending' ? this.selected.group : pickedGroup;
        const receiver: GroupModel = this.activeColumn === 'sending' ? pickedGroup : this.selected.group;
        this.communicationRulesService.checkAddLink(sender, receiver)
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    if (error.status === 409) {
                        this.notifyService.error('user.communication.add-communication.error.impossible.content'
                            , 'user.communication.add-communication.error.impossible.title');
                    } else {
                        this.notifyService.error({
                            key: 'group.internal-communication-rule.change.error.content',
                            parameters: {groupName: this.groupNameService.getGroupName(this.selected.group)}
                        }, 'group.internal-communication-rule.change.error.title');
                    }
                    return Observable.throw({printed: true, original: error});
                }),
                tap((res: any) => {
                    if (res.warning === WARNING_STARTGROUP_USERS_CAN_COMMUNICATE) {
                        this.warningGroupSender = true;
                    } else if (res.warning === WARNING_ENDGROUP_USERS_CAN_COMMUNICATE) {
                        this.warningGroupReceiver = true;
                    } else if (res.warning === WARNING_BOTH_GROUPS_USERS_CAN_COMMUNICATE) {
                        this.warningGroupSender = true;
                        this.warningGroupReceiver = true;
                    }
                    this.pickedGroup = pickedGroup;
                    this.addConfirmationDisplayed = true;
                    this.changeDetectorRef.markForCheck();
                }),
                mergeMap(() => this.$addConfirmationClicked.asObservable()),
                first(),
                tap(() => this.addConfirmationDisplayed = false),
                filter(choice => choice === 'confirm'),
                mergeMap(() => this.communicationRulesService.createCommunication(sender, receiver, this.activeColumn))
            )
            .subscribe(() => this.notifyService.success('user.communication.add-communication.success'),
                (err) => {
                    if (!err.printed) {
                        this.notifyService.error({
                            key: 'user.communication.add-communication.error.content',
                            parameters: {groupName: this.selected.group.name}
                        }, 'user.communication.add-communication.error.title');
                    }
                });
    }
}

export interface CommunicationRule {
    sender: GroupModel;
    receivers: GroupModel[];
}

export function uniqueGroups(groups: GroupModel[]): GroupModel[] {
    const uniqGroups: GroupModel[] = [];
    groups.forEach(group => {
        if (!uniqGroups.some(g => g.id === group.id)) {
            uniqGroups.push(group);
        }
    });
    return uniqGroups;
}

export function sortGroups(groups: GroupModel[], getGroupNameFn: (GroupModel) => string, activeStructureId: string): GroupModel[] {
    const alphabeticallySortedGroups = groups.filter(g => g != null).sort((a, b) => getGroupNameFn(a).localeCompare(getGroupNameFn(b)));

    const countByStructure = alphabeticallySortedGroups
        .map(g => getStructureIdOfGroup(g))
        .reduce((previousCounter, structureId) => {
            const counter = Object.assign({}, previousCounter);
            counter[structureId] = counter[structureId] ? counter[structureId] + 1 : 1;
            return counter;
        }, {});

    alphabeticallySortedGroups.sort((a, b) => {
        const structureOfA = getStructureIdOfGroup(a);
        const structureOfB = getStructureIdOfGroup(b);
        if (structureOfA === structureOfB) {
            return 0;
        }

        if (structureOfA === activeStructureId) {
            return -1;
        }

        if (structureOfB === activeStructureId) {
            return 1;
        }

        return countByStructure[structureOfB] - countByStructure[structureOfA];
    });


    return alphabeticallySortedGroups;
}

export function getStructureOfGroup(group: GroupModel): { id: string, name: string } {
    if (group && group.structures && group.structures.length > 0) {
        return group.structures[0];
    }
    return {id: '', name: ''};
}

export function getStructureIdOfGroup(group: GroupModel): string {
    return group.structureId || getStructureOfGroup(group).id;
}

export type Column = 'sending' | 'receiving';

interface Cell {
    column: Column;
    group: GroupModel;
}

