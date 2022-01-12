import { Component, Injector } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";

@Component({
    selector: 'ode-widgets-list',
    template: `
        <ode-services-list serviceName="widgets">
        </ode-services-list>
    `
})
export class WidgetsListComponent extends OdeComponent {
    constructor(injector: Injector) {
        super(injector);
    }
}