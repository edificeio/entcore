import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';


@Component({
    selector: 'ode-users-relink',
    templateUrl: './users-relink.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersRelinkComponent extends OdeComponent {
    constructor(
        injector: Injector,
    ) {
        super(injector);
    }
}
