import { Component, Input } from '@angular/core'

@Component({
    selector: 'form-field',
    template:`
        <div class="form-field">
            <label>
                {{ label | translate }}
                <message-sticker *ngIf="help" 
                            [type]="'info'" 
                            [messages]="[help]">
                </message-sticker>
            </label>
            <ng-content></ng-content>
        </div>
    `,
    styles: [`
        div.form-field { display: flex; align-items: center; }
        div.form-field >>> > * { flex: 1; margin-left: 5px; }
        div.form-field > *:first-child { flex: 0 0 200px; margin-left: 0; }
        div.form-field message-sticker { font-size: 1.3em; }
    `]
})
export class FormFieldComponent {
    @Input() label: string;
    @Input() help: string;
}