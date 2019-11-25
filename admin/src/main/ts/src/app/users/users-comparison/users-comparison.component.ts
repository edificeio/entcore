import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {UserOverview} from '../user-overview/user-overview.component';

@Component({
    selector: 'ode-users-comparison',
    templateUrl: './users-comparison.component.html',
    styleUrls: ['./users-comparison.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComparisonComponent {
    @Input()
    user1: UserOverview;

    @Input()
    user2: UserOverview;
}
