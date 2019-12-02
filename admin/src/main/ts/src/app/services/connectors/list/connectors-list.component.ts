import { OdeComponent } from './../../../core/ode/OdeComponent';
import { Component, Injector } from '@angular/core';

@Component({
    selector: 'connectors-list',
    templateUrl: './connectors-list.component.html'
})
export class ConnectorsListComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
