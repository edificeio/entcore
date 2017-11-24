import { Component, Input, Output, EventEmitter } from '@angular/core'

@Component({
    selector: 'confirm-light-box',
    template: `
        <light-box [show]="show" (onClose)="onCancel.emit()">
            <h2>{{ title | translate }}</h2>
            <div class="content">
                <ng-content></ng-content>
            </div>
            <div class="action">
                <button (click)="onConfirm.emit()" class="confirm">{{ 'confirm' | translate }}</button>
                <button (click)="onCancel.emit()" class="cancel">{{ 'cancel' | translate }}</button>
            </div>
        </light-box>
    `
})
export class ConfirmationLightbox {

    @Input('title') title: string;
    @Input('show') show: boolean;

    @Output('onConfirm') onConfirm: EventEmitter<void> =  new EventEmitter<void>();
    @Output('onCancel') onCancel: EventEmitter<void> =  new EventEmitter<void>();
}