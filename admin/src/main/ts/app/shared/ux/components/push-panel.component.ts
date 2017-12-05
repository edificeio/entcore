import { Component, Input, Output, EventEmitter, ElementRef, ViewChild } from '@angular/core'

@Component({
    selector: 'push-panel',
    template: `
    <div [ngClass]="{ opened: _opened }" #inside>
        <ng-content select="[inside]"></ng-content>
    </div>
    <div>
        <ng-content select="[companion]"></ng-content>
    </div>
    `,
    styles: [`
        :host > div:nth-child(1) {
            position: fixed;
            z-index: 10;
            overflow-x: hidden;
            overflow-y: scroll;
            height: 100%;
            top: 0px;
            left: -30%;
            width: 30%;
            transition: transform 0.25s;
        }
        :host > div:nth-child(1).opened {
            transform: translateX(100%);
        }
        :host > div:nth-child(2) {
            position: relative;
            left: 0%;
            opacity: 1;
            transition: transform 0.25s, opacity 0.25s;
        }
        :host > div:nth-child(1).opened + div {
            opacity: 0.7;
            transform: translateX(30%);
            overflow-x: hidden;
        }
    `],
    host: {
        '(document:click)': 'onClick($event)',
    }
})
export class PushPanelComponent {

    constructor(private _eref: ElementRef){}

    @Input() private set toggle(toggle: boolean){
        this._opened = toggle
    }
    _opened: boolean;
    
    @Input() private opener

    @Output() private onClose = new EventEmitter<boolean>();

    @ViewChild("inside") private inside : ElementRef

    private onClick(event) {
        let checkOpener = this.opener &&
            (this.opener.contains && this.opener.contains(event.target)) ||
            (this.opener.nativeElement && this.opener.nativeElement.contains(event.target))

        if (this._opened &&
            !this.inside.nativeElement.contains(event.target) &&
            !checkOpener){
            this._opened = false
            this.onClose.emit()
        }
        return true
    }

}
