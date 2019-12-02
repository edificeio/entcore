import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, ElementRef, EventEmitter, Input, Output, ViewChild, Injector } from '@angular/core';

@Component({
    selector: 'ode-push-panel',
    templateUrl: './push-panel.component.html',
    styleUrls: ['./push-panel.component.scss'],
    host: {
        '(document:click)': 'onClick($event)',
    }
})
export class PushPanelComponent extends OdeComponent {

    constructor(injector: Injector, private _eref: ElementRef) {
        super(injector);
    }

    @Input() private set toggle(toggle: boolean) {
        this._opened = toggle;
    }
    _opened: boolean;

    @Input() private opener;

    @Output() private onClose = new EventEmitter<boolean>();

    @ViewChild('inside', { static: false }) private inside: ElementRef;

    public onClick(event) {
        const checkOpener = this.opener &&
            (this.opener.contains && this.opener.contains(event.target)) ||
            (this.opener.nativeElement && this.opener.nativeElement.contains(event.target));

        if (this._opened &&
            !this.inside.nativeElement.contains(event.target) &&
            !checkOpener) {
            this._opened = false;
            this.onClose.emit();
        }
        return true;
    }

}
