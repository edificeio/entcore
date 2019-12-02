import { OdeComponent } from './../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';

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
