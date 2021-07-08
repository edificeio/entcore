import { Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-widgets-list',
    templateUrl: './widgets-list.component.html'
})
export class WidgetsListComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
