import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { UserOverview } from './user-overview.component';

@Component({
    selector: 'users-comparison',
    template: `
        <user-overview [user]="user1"></user-overview>
        <div class="users-comparison__separator"></div>
        <user-overview [user]="user2"></user-overview>
    `,
    styles: [`
        :host {
            display: flex;
            flex: 1;
            flex-direction: row;
        }
        user-overview {
            flex: 1;
            margin: 10px;
        }
        .users-comparison__separator {
            background: black;
            width: 1px;
            margin: 10px;
        }
    `],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComparisonComponent {
    @Input()
    user1: UserOverview;

    @Input()
    user2: UserOverview;
}
