import { OdeComponent } from './core/ode/OdeComponent';
import { Component, Injector } from '@angular/core';

@Component({
    selector: 'ode-app-home',
    templateUrl: './app-home.component.html'
})
export class AppHomeComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
