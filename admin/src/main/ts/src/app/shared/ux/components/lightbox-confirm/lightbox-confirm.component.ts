import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, EventEmitter, Input, Output, Injector } from '@angular/core';

@Component({
    selector: 'ode-lightbox-confirm',
    templateUrl: './lightbox-confirm.component.html',
    styleUrls: ['./lightbox-confirm.component.scss']
})
export class LightboxConfirmComponent extends OdeComponent {

    @Input() lightboxTitle: string;
    @Input() show: boolean;

    @Output() onConfirm: EventEmitter<void> =  new EventEmitter<void>();
    @Output() onCancel: EventEmitter<void> =  new EventEmitter<void>();

    constructor(injector: Injector) {
        super(injector);
    }
}
