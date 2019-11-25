import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
    selector: 'ode-lightbox-confirm',
    templateUrl: './lightbox-confirm.component.html',
    styleUrls: ['./lightbox-confirm.component.scss']
})
export class LightboxConfirmComponent {

    @Input('lightboxTitle') lightboxTitle: string;
    @Input('show') show: boolean;

    @Output('onConfirm') onConfirm: EventEmitter<void> =  new EventEmitter<void>();
    @Output('onCancel') onCancel: EventEmitter<void> =  new EventEmitter<void>();
}
