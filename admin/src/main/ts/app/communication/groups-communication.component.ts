import { Component, EventEmitter, Input, Output } from '@angular/core';
import { GroupModel, StructureModel } from '../core/store/models';
import { CommunicationRule } from './communication-rules.component';

const css = {
    title: 'lct-groups-communication-title',
    backButton: 'lct-groups-communication-back-button'
};

export const groupsCommunicationLocators = {
    title: `.${css.title}`,
    backButton: `.${css.backButton}`
};

@Component({
    selector: 'groups-communication',
    template: `
        <div class="panel-header">
            <button type="button" class="${css.backButton} button button--back" (click)="close.emit();">
                <i class="fa fa-arrow-left"></i>
                <span>{{ backButtonLabel }}</span>
            </button>
            <div>
                <i class="fa fa-podcast"></i>
                <span class="${css.title}">{{ title }}</span>
                <message-sticker class="is-pulled-right" [type]="'info'" [messages]="['user.communication.help']"></message-sticker>
            </div>
        </div>
        
        <panel-section [section-title]="sendingSectionTitle" [folded]="false">
            <div panel-section-header-icons>
                <i class='fa fa-user'></i> <i class='fa fa-arrow-right'></i> <strong>?</strong>
            </div>

            <div class="groups-communication__content">
                <communication-rules
                    [activeStructure]="activeStructure"
                    [sendingHeaderLabel]="sendingSectionSendingColumnLabel"
                    [receivingHeaderLabel]="sendingSectionReceivingColumnLabel"
                    [communicationRules]="sendingCommunicationRules"
                    [activeColumn]="'sending'"
                    [manageableStructuresId]="manageableStructuresId"
                    [addCommunicationPickableGroups]="addCommunicationPickableGroups"
                    [structures]="structures"
                    (groupPickerStructureChange)="groupPickerStructureChange.emit($event)"></communication-rules>
            </div>
        </panel-section>
        
        <panel-section [section-title]="receivingSectionTitle" [folded]="false">
            <div panel-section-header-icons>
                <strong>?</strong> <i class='fa fa-arrow-right'></i> <i class='fa fa-user'></i>
            </div>
            
            <div class="groups-communication__content">
                <communication-rules
                    [activeStructure]="activeStructure"
                    [sendingHeaderLabel]="receivingSectionSendingColumnLabel"
                    [receivingHeaderLabel]="receivingSectionReceivingColumnLabel"
                    [communicationRules]="receivingCommunicationRules"
                    [activeColumn]="'receiving'"
                    [manageableStructuresId]="manageableStructuresId"
                    [addCommunicationPickableGroups]="addCommunicationPickableGroups"
                    [structures]="structures"
                    (groupPickerStructureChange)="groupPickerStructureChange.emit($event)"></communication-rules>
            </div>
        </panel-section>`,
    styles: [`
        .button.button--back {
            background: none;
            border: 0;
            padding: 5px;
            font-size: 1.2rem;
        }
    `, `
        .button.button--back:hover, .button.button--back:hover i {
            color: #ff5e1f !important;
        }
    `, `
        .button.button--back i {
            float: none;
            padding-left: 0;
        }
    `, `
        .groups-communication__content {
            padding: 10px 15px;
        }
    `]
})
export class GroupsCommunicationComponent {
    @Input()
    public title: string = '';

    @Input()
    public backButtonLabel: string = '';

    @Input()
    public sendingSectionTitle: string = '';

    @Input()
    public sendingSectionSendingColumnLabel: string = '';

    @Input()
    public sendingSectionReceivingColumnLabel: string = '';

    @Input()
    public receivingSectionTitle: string = '';

    @Input()
    public receivingSectionSendingColumnLabel: string = '';

    @Input()
    public receivingSectionReceivingColumnLabel: string = '';

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
    public close: EventEmitter<void> = new EventEmitter<void>();

    @Output()
    public groupPickerStructureChange: EventEmitter<StructureModel> = new EventEmitter<StructureModel>();
}
