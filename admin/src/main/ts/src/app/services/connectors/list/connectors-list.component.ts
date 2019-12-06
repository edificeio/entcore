import { Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-connectors-list',
    templateUrl: './connectors-list.component.html'
})
export class ConnectorsListComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
