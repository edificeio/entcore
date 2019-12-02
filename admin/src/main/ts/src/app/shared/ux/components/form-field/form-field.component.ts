import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, Input, Injector } from '@angular/core';

@Component({
    selector: 'ode-form-field',
    templateUrl: './form-field.component.html',
    styleUrls: ['./form-field.components.scss']
})
export class FormFieldComponent extends OdeComponent{
    @Input() label: string;
    @Input() help: string;
    constructor(injector: Injector) {
        super(injector);
    }
}
