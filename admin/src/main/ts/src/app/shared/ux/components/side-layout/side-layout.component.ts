import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, EventEmitter, Input, Output, Injector } from '@angular/core';

@Component({
    selector: 'ode-side-layout',
    templateUrl: './side-layout.component.html',
    styleUrls: ['./side-layout.component.scss']
})
export class SideLayoutComponent extends OdeComponent {
    @Input() showCompanion = false;
    @Output('closeCompanion') close: EventEmitter<void> = new EventEmitter<void>();
    constructor(injector: Injector) {
        super(injector);
    }
}
