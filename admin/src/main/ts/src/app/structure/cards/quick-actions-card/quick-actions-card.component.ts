import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-quick-actions-card',
    templateUrl: './quick-actions-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickActionsCardComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
