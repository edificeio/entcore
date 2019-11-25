import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';


export interface Option {
  value: string;
  label: string;
}


@Component({
    selector: 'ode-simple-select',
    templateUrl: './simple-select.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleSelectComponent {
    @Input() selected: string;
    @Input() model: {[key: string]: string};
    @Input() options: Option[];
    @Input() ignoreOption: Option[];
    @Output() selectChange: EventEmitter<string> = new EventEmitter<string>();
}

