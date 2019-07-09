import { Component, ElementRef, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'side-panel',
    template: `
        <div [ngClass]="{ opened: opened }">
            <ng-content></ng-content>
        </div>`,
    styles: [`
        div {
            position: fixed;
            z-index: 10;
            overflow-x: hidden;
            overflow-y: scroll;
            top: 0;
            left: -30%;
            width: 30%;
            transition: transform 0.25s;
        }

        div.opened {
            transform: translateX(100%);
        }
    `],
    host: {
        '(document:click)': 'onClick($event)',
    }
})
export class SidePanelComponent {
    opened: boolean;

    constructor(private elementRef: ElementRef) {
    }

    @Input() set toggle(toggle: boolean) {
        this.opened = toggle;
    }

    @Input() opener;
    @Output() onClose = new EventEmitter<boolean>();

    onClick(event) {
        let checkOpener = this.opener &&
            (this.opener.contains && this.opener.contains(event.target)) ||
            (this.opener.nativeElement && this.opener.nativeElement.contains(event.target));

        if (this.opened &&
            !this.elementRef.nativeElement.contains(event.target) &&
            !checkOpener) {
            this.opened = false;
            this.onClose.emit();
        }
        return true;
    }

}
