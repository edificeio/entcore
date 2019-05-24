import { Component, Input, Output, ChangeDetectorRef, ElementRef, EventEmitter, OnInit } from '@angular/core'

export type MessageType = 'info' | 'success' | 'warning' | 'danger';

export const icons = {
    info : 'fa-question-circle',
    warning : 'fa-exclamation-circle',
    danger : 'fa-ban',
    success : 'fa-check-circle' 
};

@Component({
    selector: 'message-box',
    template: `
        <article class="message is-{{type}}" [hidden]="hidden">
            <div *ngIf="header" class="message-header">
                <p>
                <i *ngIf="canHide()" (click)="hide()" class="fa fa-times-circle"></i>
                    {{header | translate}}
                </p>
            </div>
            <div class="message-body">
                <i *ngIf="canHide() && !header" (click)="hide()" class="fa fa-times-circle"></i>
                <p *ngFor="let message of messages"> 
                    <s5l [s5l-params]="message[1]">{{message[0]}}</s5l>
                </p>
            </div>
        </article>
        `,
    styles: [`
        :host { 
            font-size : initial;
            position: inherit;
            top: -5px;
            right: -5px; 
        }
        :host .message {
            border: 1px solid;
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
        private eltRef:ElementRef,
        private cdRef : ChangeDetectorRef)  {}

    @Input() type: MessageType;
    @Input() header:string;

    private _messages:[string,Object][];
    @Input() set messages(value:(string | [string,Object])[]) {
        this._messages = [];

        for (let message of value) {
            if (typeof message == "string")
                this._messages.push([message,{}]);
            else if (typeof message == "object") {
                this._messages.push(message);
            }
        }
    };
    get messages() {
        return this._messages
    }

    private _position:'absolute' | 'inherit' = 'inherit';
    @Input() set position(value:'absolute' | 'inherit') {
        this._position = value;
    }

    hidden:boolean = false;
    @Output('onHide') hideEvent: EventEmitter<void>;
    canHide():boolean {
        if (this._position == 'absolute')
            return true; 
        return false;
    }
    hide(): void {
        this.hidden = true;
        this.hideEvent.emit();
    }

    ngOnInit():void { 
        if (this.type == undefined) {
            throw new Error('MessageSticker : type\' property must be set');
        }
        this.eltRef.nativeElement.style.position = this._position;
        if (this._position == 'absolute') {
            this.eltRef.nativeElement.style.width = '450px';
            // TODO: Maybe use a static counter to push on top the last open MessageBox ?
            this.eltRef.nativeElement.style.zIndex = '10';
        }
    }
}

