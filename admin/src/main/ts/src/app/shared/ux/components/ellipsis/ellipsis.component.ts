import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, ElementRef, Input, Injector } from '@angular/core';

type EllipsisBehavior = 'expand' | 'hide';

/**
 * A simple ellipsis component that switches text-overflow on click.
 * Add an ellipsis={'hide'| 'expand'} attirubute on your element.
 * Default behavior is 'hide', so that it's the same than the raw CSS
 */
@Component({
    selector: '[ellipsis]',
    template: `<ng-content></ng-content>`,
    styleUrls: ['./ellipsis.component.scss'],
    host: {
        '(click)': 'onClick()',
    }
})
export class EllipsisComponent extends OdeComponent {
    constructor(private eltRef: ElementRef, injector: Injector ) {
        super(injector);
    }

    private _ellipsis: EllipsisBehavior  = 'hide';
    @Input()
    set ellipsis(value: EllipsisBehavior) {
        if (!!value) {
            this._ellipsis = value;
            if (this._ellipsis == 'expand') {
                this.eltRef.nativeElement.className = 'expand';
            }
        }
    }

    private isExpanded = false;

    private initialStyle = Object.assign({}, this.eltRef.nativeElement.style);
    onClick(): void {
        if (this._ellipsis == 'hide') {
            return;
        }
        if (!this.isExpanded) {
            this.isExpanded = true;
            Object.assign(this.eltRef.nativeElement.style, {
                textOverflow: 'clip',
                overflow: 'auto',
                whiteSpace: 'normal',
            });
        } else {
            this.isExpanded = false;
            this.eltRef.nativeElement.style = this.initialStyle;
        }

    }

}
