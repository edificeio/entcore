import { Component, Input, Output, EventEmitter } from '@angular/core'

@Component({
    selector: 'lightbox-confirm',
    template: `
        <lightbox [show]="show" (onClose)="onCancel.emit()">
            <h2 class="lightbox-confirm__header">{{ lightboxTitle | translate }}</h2>
            <div class="lightbox-confirm-content">
                <ng-content></ng-content>
            </div>
            <div class="lightbox-confirm-action">
                <button (click)="onConfirm.emit()" class="confirm">{{ 'confirm' | translate }}</button>
                <button (click)="onCancel.emit()" class="cancel">{{ 'cancel' | translate }}</button>
            </div>
        </lightbox>
    `,
    styles: [`
        .lightbox-confirm__header {
            margin-bottom: 0;
        }
        .lightbox-confirm-content {
            font-size: 16px;
            padding: 20px 0;
        }
        .lightbox-confirm-action button {
            float: right;
            text-align: center;
            margin-left: 5px;
            margin-bottom: 20px;
        }
    `]
})
export class LightboxConfirmComponent {

    @Input('lightboxTitle') lightboxTitle: string;
    @Input('show') show: boolean;

    @Output('onConfirm') onConfirm: EventEmitter<void> =  new EventEmitter<void>();
    @Output('onCancel') onCancel: EventEmitter<void> =  new EventEmitter<void>();
}