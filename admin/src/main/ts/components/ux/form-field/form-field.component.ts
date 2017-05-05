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
        div.form-field { display: flex; }
        div.form-field >>> > * { flex: 1; }
        div.form-field > *:first-child { flex: 0 0 200px; }
    `]
})
export class FormField{
    @Input() label: string
}