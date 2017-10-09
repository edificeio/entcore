import { Component, Input,ChangeDetectorRef, ViewChild, OnInit } from '@angular/core'
import { ComponentDescriptor, DynamicComponent } from '../../directives/dynamic-component'
import { BundlesService } from 'sijil'
import { MessageBoxComponent, MessageType, icons } from './message-box.component'

@Component({
    selector: 'message-sticker',
    template: `
        <i (click)="loadMessageBox()" class="fa {{icons[type]}} is-{{type}}"></i>
        <ng-template [dynamic-component]="newMessageBox()"></ng-template>
        `,
    styles: [`
        :host {
            display: inline;
            position: absolute;
            padding-left : .5em;
        }
        i { cursor : pointer; }
    `]
})
export class MessageStickerComponent implements OnInit {
    constructor (
        private cdRef:ChangeDetectorRef)  {}

    @Input() type: MessageType;
    @Input() header:string;
    @Input() messages:string[];
    @ViewChild(DynamicComponent) dComponent: DynamicComponent;

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
            position:'absolute'
        });
    }

    loadMessageBox() : void {
        this.dComponent.load();
    }

}