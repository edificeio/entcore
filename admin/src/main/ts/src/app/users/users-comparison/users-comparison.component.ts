import { OdeComponent } from './../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, Input, Injector } from '@angular/core';
import {UserOverview} from '../user-overview/user-overview.component';

@Component({
    selector: 'ode-users-comparison',
    templateUrl: './users-comparison.component.html',
    styleUrls: ['./users-comparison.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComparisonComponent extends OdeComponent {
    @Input()
    user1: UserOverview;

    @Input()
    user2: UserOverview;
    constructor(injector: Injector) {
        super(injector);
    }
}
