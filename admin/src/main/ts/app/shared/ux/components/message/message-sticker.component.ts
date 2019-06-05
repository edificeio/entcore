import { Component, Input, ViewChild, OnInit, EventEmitter } from '@angular/core'
import { ComponentDescriptor, DynamicComponentDirective } from '../../directives'
import { MessageBoxComponent, MessageType, icons } from './message-box.component'

@Component({
    selector: 'message-sticker',
    template: `
    <i (click)="loadMessageBox()" class="fa {{icons[type]}} is-{{type}}"></i>
        <span message-box-anchor>
            <ng-container [dynamic-component]="newMessageBox()"></ng-container>
        </span>
        `,
    styles: [`
        :host {
            display: inline;
            padding-left : .2em;
        }
        span[message-box-anchor] {
            position: absolute;
            z-index: 8;
        }
        i { cursor : pointer; }
    `]
})
export class MessageStickerComponent implements OnInit {
    @Input() type: MessageType;
    @Input() header:string;
    @Input() messages:(string | [string,Object])[];
    @ViewChild(DynamicComponentDirective) dComponent: DynamicComponentDirective;

    readonly icons = icons;
    
    ngOnInit():void { 
        if (this.type == undefined) {
            throw new Error('MessageSticker : type\' property must be set');
        }
    }

    newMessageBox():ComponentDescriptor {
        return new ComponentDescriptor(MessageBoxComponent, {
            type: this.type,
            header: this.header, 
            messages:this.messages,
            position:'absolute',
            hideEvent: new EventEmitter<void>()
        });
    }

    loadMessageBox() : void {
        this.dComponent.load();
    }
}