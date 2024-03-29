import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-create-message-flash',
    template: '<ode-message-flash-form action="create"></ode-message-flash-form>',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateMessageFlashComponent extends OdeComponent {

    constructor(injector: Injector) {
        super(injector);
    }

}
