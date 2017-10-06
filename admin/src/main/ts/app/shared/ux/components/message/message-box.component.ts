import { Component, Input, Output, ChangeDetectorRef, ElementRef, EventEmitter, OnInit } from '@angular/core'
import { BundlesService } from 'sijil'

export type MessageType = 'info' | 'success' | 'warning' | 'danger';

@Component({
    selector: 'message-box',
    template: `
        <article class="message is-{{type}}" [hidden]="hidden">
            <div *ngIf="header" class="message-header">
                <p>
                <i *ngIf="canHide()" (click)="hidden = true" class="fa fa-times-circle"></i>
                    {{header}}
                </p>
            </div>
            <div class="message-body">
            <i *ngIf="canHide() && !header" (click)="hidden = true" class="fa fa-times-circle"></i>
                <p *ngFor="let message of messages"> 
                {{message | translate}}
                </p>
            </div>
        </article>
        `,
    styles: [`
        :host { font-size : initial; }
        article.message { 
            position: inherit;
            top: -5px;
            right: -5px; 
        }
        i.fa-times-circle {
            display:inline-flex;
            float: right;
            cursor: pointer;
            font-weight: bold;
        }
    `]
})
export class MessageBoxComponent implements OnInit {
    constructor (
        private bundles: BundlesService,
        private eltRef:ElementRef,
        private cdRef : ChangeDetectorRef)  {}

    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    @Input() type: MessageType;
    @Input() header:string;
    @Input() messages:string[];

    private _position:'absolute' | 'inherit' = 'inherit';
    @Input() set position(value:'absolute' | 'inherit') {
        this._position = value;
    }


    hidden:boolean = false ;
    canHide():boolean {
        if (this._position == 'absolute')
            return true; 
        return false;
    }

    ngOnInit():void { 
        if (this.type == undefined) {
            throw new Error('MessageSticker : type\' property must be set');
        }
        this.eltRef.nativeElement.style.position = this._position;
    }
}

