import { Component, Input } from '@angular/core'
import { AbstractControl } from '@angular/forms'

@Component({
    selector: 'form-errors',
    template: `
        <div *ngIf="ref && ref.errors && (ref.dirty || ref.touched)" class="form-errors">
            <div *ngFor="let error of getErrorsArray()">
                <span>{{ 'form.error.' + error.name | translate: error.value }}</span>
            </div>
        </div>
    `,
    styles: [`
        .form-errors {
            font-size: 0.8em;
            color: crimson;
            font-style: italic;
        }
    `]
})
export class FormErrors {
    constructor(){}

    @Input("control")
    private ref : AbstractControl

    private getErrorsArray() {
        let errorsArray = []
        for(let prop in this.ref.errors) {
            errorsArray.push({ name: prop, value: this.ref.errors[prop]})
        }
        return errorsArray
    }
}