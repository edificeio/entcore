import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-services-card',
    templateUrl: './services-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServicesCardComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
