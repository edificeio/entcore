import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommunicationRule} from '../communication-rules.component/communication-rules.component';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { GroupModel } from 'src/app/core/store/models/group.model';

const css = {
    title: 'lct-groups-communication-title',
    backButton: 'lct-groups-communication-back-button'
};

export const groupsCommunicationLocators = {
    title: `.${css.title}`,
    backButton: `.${css.backButton}`
};

@Component({
    selector: 'ode-groups-communication',
    templateUrl: './groups.communication.component.html',
    styleUrls: ['./groups-communication.component.scss']
})
export class GroupsCommunicationComponent {
    get sendingCommunicationRules(): CommunicationRule[] {
        return this._sendingCommunicationRules;
    }

    @Input()
    set sendingCommunicationRules(value: CommunicationRule[]) {
        console.log('sending rules ', value)
        this._sendingCommunicationRules = value;
    }
    get receivingCommunicationRules(): CommunicationRule[] {
        return this._receivingCommunicationRules;
    }
    @Input()
    set receivingCommunicationRules(value: CommunicationRule[]) {
        console.log('receiving rules', value)
        this._receivingCommunicationRules = value;
    }
    @Input()
    public title = '';

    @Input()
    public backButtonLabel = '';

    @Input()
    public sendingSectionTitle = '';

    @Input()
    public sendingSectionSendingColumnLabel = '';

    @Input()
    public sendingSectionReceivingColumnLabel = '';

    @Input()
    public receivingSectionTitle = '';

    @Input()
    public receivingSectionSendingColumnLabel = '';

    @Input()
    public receivingSectionReceivingColumnLabel = '';

    @Input()
    public activeStructure: StructureModel;

    @Input()
    public manageableStructuresId: string[];

    private _sendingCommunicationRules: CommunicationRule[];

    @Input()
    public addCommunicationPickableGroups: GroupModel[];


    private _receivingCommunicationRules: CommunicationRule[];

    @Input()
    public structures: StructureModel[];

    @Output()
    public groupClose: EventEmitter<void> = new EventEmitter<void>();

    @Output()
    public groupPickerStructureChange: EventEmitter<StructureModel> = new EventEmitter<StructureModel>();
}
