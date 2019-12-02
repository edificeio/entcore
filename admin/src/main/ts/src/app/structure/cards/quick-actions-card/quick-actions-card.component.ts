import { OdeComponent } from './../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';

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
