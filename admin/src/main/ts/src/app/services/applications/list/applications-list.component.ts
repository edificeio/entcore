import { OdeComponent } from './../../../core/ode/OdeComponent';
import { Component, Injector } from '@angular/core';

@Component({
    selector: 'apps-list',
    templateUrl: './application-list.component.html'
})
export class ApplicationsListComponent extends OdeComponent {

    constructor(injector: Injector) {
        super(injector);
    }

}
