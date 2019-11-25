import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
    selector: 'ode-side-layout',
    templateUrl: './side-layout.component.html',
    styleUrls: ['./side-layout.component.scss']
})
export class SideLayoutComponent {
    @Input() showCompanion = false;
    @Output('closeCompanion') close: EventEmitter<void> = new EventEmitter<void>();
}
