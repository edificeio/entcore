import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {BundlesService} from 'sijil';

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

@Component({
    selector: 'ode-user-overview',
    templateUrl: './user-overview.component.html',
    styleUrls: ['./user-overview.component.scss'],
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
