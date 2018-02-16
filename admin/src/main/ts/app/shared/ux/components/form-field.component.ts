import { Component, Input } from '@angular/core'

@Component({
    selector: 'form-field',
    template:`
        <div class="form-field">
            <label>{{ label | translate }}</label>
            <ng-content></ng-content>
        </div>
    `,
    styles: [`
        div.form-field { display: flex; align-items: center; }
        div.form-field >>> > * { flex: 1; }
        div.form-field > *:first-child { flex: 0 0 150px; }
    `]
})
export class FormFieldComponent {
    @Input() label: string
}