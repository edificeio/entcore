import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, ElementRef, EventEmitter, Input, Output, Injector } from '@angular/core';

@Component({
    selector: 'ode-side-panel',
    templateUrl: './side-panel.component.html',
    styleUrls: ['./side-panel.component.scss'],
    host: {
        '(document:click)': 'onClick($event)',
    }
})
export class SidePanelComponent extends OdeComponent {
    opened: boolean;

    constructor(injector: Injector, private elementRef: ElementRef) {
        super(injector);
    }

    @Input() set toggle(toggle: boolean) {
        this.opened = toggle;
    }

    @Input() opener;
    @Output() onClose = new EventEmitter<boolean>();

    onClick(event) {
        const checkOpener = this.opener &&
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
