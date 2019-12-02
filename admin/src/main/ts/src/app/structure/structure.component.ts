import { OdeComponent } from './../core/ode/OdeComponent';
import { Component, Injector } from '@angular/core';

@Component({
    selector: 'ode-structure',
    template: '<router-outlet></router-outlet>'
})
export class StructureComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
