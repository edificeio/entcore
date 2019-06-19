import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BundlesService } from 'sijil';

export interface UserOverview {
    displayName: string;
    type: string;
    activationCode: string;
    firstName: string;
    lastName: string;
    login: string;
    birthDate: string;
    email: string;
    source: string;
    structures: string[];
}

export const locators = {
    displayName: '.lct-user-overview-display-name',
    type: '.lct-user-overview-type',
    activationCode: '.lct-user-overview-activation-code',
    firstName: '.lct-user-overview-first-name',
    lastName: '.lct-user-overview-last-name',
    login: '.lct-user-overview-login',
    birthDate: '.lct-user-overview-birth-date',
    email: '.lct-user-overview-email',
    source: '.lct-user-overview-source',
    structures: '.lct-user-overview-structures',
    label: '.lct-user-overview-label',
    value: '.lct-user-overview-value',
};

@Component({
    selector: 'user-overview',
    template: `
        <div class='user-overview__header'>
            <div class="user-overview__row">
                <span class="user-overview__display-name lct-user-overview-display-name">{{user.displayName}}</span>
                <span class="user-overview__type lct-user-overview-type">{{user.type | translate}}</span>
            </div>
            <div class="user-overview__row user-overview__activation-code lct-user-overview-activation-code">{{ ((!!user.activationCode && user.activationCode.length > 0) ? 'user.is.inactive' : 'user.is.active') | translate }}</div>
        </div>

        <div class="user-overview__row lct-user-overview-first-name">
            <span class="user-overview__label lct-user-overview-label">{{'firstName' | translate}}</span>
            <span class="user-overview__value lct-user-overview-value">{{user.firstName}}</span>
        </div>
        <div class="user-overview__row lct-user-overview-last-name">
            <span class="user-overview__label lct-user-overview-label">{{'lastName' | translate}}</span>
            <span class="user-overview__value lct-user-overview-value">{{user.lastName}}</span>
        </div>
        <div class="user-overview__row lct-user-overview-login">
            <span class="user-overview__label lct-user-overview-label">{{'login' | translate}}</span>
            <span class="user-overview__value lct-user-overview-value">{{user.login}}</span>
        </div>
        <div class="user-overview__row lct-user-overview-birth-date">
            <span class="user-overview__label lct-user-overview-label">{{'birthDate' | translate}}</span>
            <span class="user-overview__value lct-user-overview-value">{{displayDate(user.birthDate)}}</span>
        </div>
        <div class="user-overview__row lct-user-overview-email">
            <span class="user-overview__label lct-user-overview-label">{{'email' | translate}}</span>
            <span class="user-overview__value lct-user-overview-value">{{user.email}}</span>
        </div>
        <div class="user-overview__row lct-user-overview-source">
            <span class="user-overview__label lct-user-overview-label">{{'source' | translate}}</span>
            <span class="user-overview__value lct-user-overview-value">{{user.source | translate}}</span>
        </div>
        <div class="user-overview__row lct-user-overview-structures">
            <span class="user-overview__label lct-user-overview-label">{{'structures' | translate}}</span>
            <div class="user-overview__column">
                <span *ngFor="let structure of user.structures;"
                      class="user-overview__value lct-user-overview-value">{{structure}}</span>
            </div>
        </div>

    `,
    styles: [`
        :host {
            display: flex;
            flex-direction: column;
        }

        .user-overview__row {
            display: flex;
            align-items: center;
        }

        .user-overview__column {
            text-align: right;
        }

        .user-overview__label, .user-overview__display-name {
            flex-grow: 1;
        }

        .user-overview__label, .user-overview__activation-code {
            margin: 0 15px;
        }

        .user-overview__display-name {
            font-size: 1.6em;
        }

        .user-overview__type {
            font-style: italic;
        }

    `],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserOverviewComponent {
    @Input()
    user: UserOverview;

    constructor(private bundlesService: BundlesService) {
    }

    displayDate(date: string): string {
        return new Date(date).toLocaleDateString(this.bundlesService.currentLanguage);
    }
}
