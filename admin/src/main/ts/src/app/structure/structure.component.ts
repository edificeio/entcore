import { Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-structure',
    template: '<router-outlet></router-outlet>'
})
export class StructureComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}
