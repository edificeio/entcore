import {Component, Input} from '@angular/core';

@Component({
    selector: 'ode-form-field',
    templateUrl: './form-field.component.html',
    styleUrls: ['./form-field.components.scss']
})
export class FormFieldComponent {
    @Input() label: string;
    @Input() help: string;
}
