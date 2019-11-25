import {Component, EventEmitter, Input, Output} from '@angular/core';
import {GroupModel, StructureModel} from '../../core/store/models';
import {CommunicationRule} from '../communication-rules.component/communication-rules.component';

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

    @Input()
    public sendingCommunicationRules: CommunicationRule[];

    @Input()
    public addCommunicationPickableGroups: GroupModel[];

    @Input()
    public receivingCommunicationRules: CommunicationRule[];

    @Input()
    public structures: StructureModel[];

    @Output()
    public groupClose: EventEmitter<void> = new EventEmitter<void>();

    @Output()
    public groupPickerStructureChange: EventEmitter<StructureModel> = new EventEmitter<StructureModel>();
}
