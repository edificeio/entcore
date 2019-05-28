import { Component, EventEmitter, Input, Output } from '@angular/core';
import { UserModel, GroupModel } from '../../core/store/models';
import { CommunicationRule } from './communication-rules.component';

const css = {
    title: 'lct-user-communication-title',
    backButton: 'lct-user-communication-back-button'
};

export const userCommunicationLocators = {
    title: `.${css.title}`,
    backButton: `.${css.backButton}`
};

@Component({
    selector: 'user-communication',
    template: `
        <div class="panel-header">
            <button type="button" class="${css.backButton} button button--back" (click)="close.emit();">
                <i class="fa fa-arrow-left"></i>
                <span>{{ 'user.communication.back-to-user-details' | translate }}</span>
            </button>
            <div>
                <i class="fa fa-podcast"></i>
                <span class="${css.title} user-displayname">{{ 'user.communication.title' | translate:{firstName: user.firstName, lastName: user.lastName.toUpperCase()} }}</span>
                <message-sticker class="is-pulled-right" [type]="'info'" [messages]="['user.communication.help']"></message-sticker>
            </div>
        </div>
        
        <panel-section section-title="user.communication.section.title.sending-rules" [folded]="false">
            <div panel-section-header-icons>
                <i class='fa fa-user'></i> <i class='fa fa-arrow-right'></i> <strong>?</strong>
            </div>

            <div class="user-communication__content">
                <communication-rules
                [sendingHeaderLabel]="'user.communication.groups-of-user' | translate"
                [receivingHeaderLabel]="'user.communication.groups-that-user-can-communicate-with' | translate"
                [communicationRules]="userSendingCommunicationRules"
                [activeColumn]="'sending'"
                [addCommunicationPickableGroups]="addCommunicationPickableGroups"></communication-rules>
            </div>
        </panel-section>
        
        <panel-section section-title="user.communication.section.title.receiving-rules" [folded]="false">
            <div panel-section-header-icons>
                <strong>?</strong> <i class='fa fa-arrow-right'></i> <i class='fa fa-user'></i>
            </div>
            
            <div class="user-communication__content">
                <communication-rules
                [sendingHeaderLabel]="'user.communication.groups-that-can-communicate-with-user' | translate"
                [receivingHeaderLabel]="'user.communication.groups-of-user' | translate"
                [communicationRules]="userReceivingCommunicationRules"
                [activeColumn]="'receiving'"
                [addCommunicationPickableGroups]="addCommunicationPickableGroups"></communication-rules>
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
        .user-communication__content {
            padding: 10px 15px;
        }
    `]
})
export class UserCommunicationComponent {

    @Input()
    public user: UserModel;

    @Input()
    public userSendingCommunicationRules: CommunicationRule[];

    @Input()
    public addCommunicationPickableGroups: GroupModel[];

    @Input()
    public userReceivingCommunicationRules: CommunicationRule[];

    @Output()
    public close: EventEmitter<void> = new EventEmitter<void>();
}
