import { Component, EventEmitter, Input, Output } from '@angular/core';
import { UserModel } from '../../core/store/models';
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
        <div class="user-communication__content">
            <communication-rules [communicationRules]="userSendingCommunicationRules"></communication-rules>
        </div>`,
    styles: [`
        .button.button--back {
            background: none;
            border: 0;
            padding: 5px;
        }
    `, `
        .button.button--back:hover, .button.button--back:hover i {
            color: #ff5e1f;
        }
    `, `
        .button.button--back i {
            float: none;
            padding-left: 0;
            transition: background 0.25s, color 0.25s, border 0.25s;
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

    @Output()
    public close: EventEmitter<void> = new EventEmitter<void>();
}
