import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, Injector } from '@angular/core';

@Component({
    selector: 'ode-portal',
    templateUrl: './portal.component.html',
    styleUrls: ['./portal.component.scss']
})
export class PortalComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
