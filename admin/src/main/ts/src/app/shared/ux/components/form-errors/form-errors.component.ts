import {Component, Input} from '@angular/core';
import {AbstractControl} from '@angular/forms';

import {LabelsService} from '../../services/labels.service';

@Component({
    selector: 'ode-form-errors',
    templateUrl: './form-errors.component.html',
    styleUrls: ['./form-errors.component.scss']
})
export class FormErrorsComponent {
    constructor(private labelsService: LabelsService) {}

    @Input('control')
    ref: AbstractControl;

    @Input('expectedPatternMsg')
    expectedPatternMsg: string;

    getErrorsArray() {
        const errorsArray = [];
        for (const prop in this.ref.errors) {
            errorsArray.push({ name: prop, value: this.ref.errors[prop]});
        }
        return errorsArray;
    }

    labels(label) {
        return this.labelsService.getLabel(label);
    }
}
