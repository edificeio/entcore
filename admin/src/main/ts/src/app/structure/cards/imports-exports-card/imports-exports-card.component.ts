import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-imports-exports-card',
    templateUrl: './imports-exports-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImportsExportsCardComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
