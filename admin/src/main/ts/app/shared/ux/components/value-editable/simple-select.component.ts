import { ChangeDetectionStrategy, Component, Input, Output, EventEmitter } from '@angular/core'

@Component({
    selector: 'simple-select',
    template: `
    <select [ngModel]="model[selected]" (ngModelChange)="model[selected] = $event; selectChange.emit($event)">
        <option *ngIf="ignoreOption" [ngValue]="ignoreOption.value">
            {{ignoreOption.label}}
        </option>
        <option *ngFor="let option of options | orderBy: 'label'" [ngValue]="option.value">
            {{option.label}}
        </option>
    </select>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleSelectComponent {
    @Input() selected: string;
    @Input() model : {[key:string]: string};
    @Input() options : Option[];
    @Input() ignoreOption: Option[];
    @Output() selectChange: EventEmitter<string> = new EventEmitter<string>();
}

export type Option = {value: string, label: string}