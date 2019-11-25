import {Directive, HostListener, Input} from '@angular/core';

@Directive({ selector: '[anchor]' })
export class AnchorDirective {

    @Input('anchor') anchor: string;
    @Input('offset') offset = 50;

    @HostListener('click')
    onClick() {
        const element = document.getElementById(this.anchor);
        window.scrollBy(0, element.getBoundingClientRect().top - this.offset);
    }

}
