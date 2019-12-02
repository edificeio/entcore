import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, EventEmitter, Input, OnInit, ViewChild, Injector } from '@angular/core';
import {ComponentDescriptor, DynamicComponentDirective} from '../../directives';
import {icons, MessageBoxComponent, MessageType} from './message-box/message-box.component';

@Component({
    selector: 'ode-message-sticker',
    templateUrl: './message-sticker.component.html',
    styleUrls: ['./message-box/message-box.component.scss']
})
export class MessageStickerComponent extends OdeComponent implements OnInit {
    @Input() type: MessageType;
    @Input() header: string;
    @Input() messages: (string | [string, {}])[];
    @ViewChild(DynamicComponentDirective, { static: false }) dComponent: DynamicComponentDirective;

    readonly icons = icons;

    constructor(injector: Injector) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        if (this.type === undefined) {
            throw new Error('MessageSticker : type\' property must be set');
        }
    }

    newMessageBox(): ComponentDescriptor {
        return new ComponentDescriptor(MessageBoxComponent, {
            type: this.type,
            header: this.header,
            messages: this.messages,
            position: 'absolute',
            hideEvent: new EventEmitter<void>()
        });
    }

    loadMessageBox(): void {
        this.dComponent.load();
    }
}
