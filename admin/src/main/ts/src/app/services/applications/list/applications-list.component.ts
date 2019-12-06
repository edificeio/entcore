import { Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-apps-list',
    templateUrl: './application-list.component.html'
})
export class ApplicationsListComponent extends OdeComponent {

    constructor(injector: Injector) {
        super(injector);
    }

}
