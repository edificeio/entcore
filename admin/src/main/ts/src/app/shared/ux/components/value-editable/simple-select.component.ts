import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, Injector } from '@angular/core';


export interface Option {
  value: string;
  label: string;
}


@Component({
    selector: 'ode-simple-select',
    templateUrl: './simple-select.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleSelectComponent extends OdeComponent {
    @Input() selected: string;
    @Input() model: {[key: string]: string};
    @Input() options: Option[];
    @Input() ignoreOption: Option[];
    @Output() selectChange: EventEmitter<string> = new EventEmitter<string>();
    constructor(injector: Injector) {
      super(injector);
    }
}

