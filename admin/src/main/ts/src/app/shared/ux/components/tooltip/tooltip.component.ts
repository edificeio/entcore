import {Component, ElementRef, Input, Renderer2} from '@angular/core';

@Component({
    selector: '[tooltip]',
    template: `<ng-content></ng-content>`,
    styleUrls: ['./tooltip.component.scss'],
    host: {
        '(mouseenter)': 'onMouseEnter()',
        '(mouseleave)': 'onMouseLeave()'
    }
})
export class TooltipComponent {

    constructor(private ref: ElementRef,
                private renderer: Renderer2) {}

    @Input('tooltip') tooltipContents: string;
    @Input() position: 'top' | 'left' | 'right' | 'bottom' = 'bottom';
    @Input() offset = 5;

    private tooltipElt: HTMLElement;

    onMouseEnter(): void {
        const r = this.renderer;

        if (!this.tooltipElt) {
            this.tooltipElt = this.renderer.createElement('div');
            let tooltipText = this.renderer.createText(this.tooltipContents);
            this.renderer.appendChild(this.tooltipElt, tooltipText);
            this.renderer.addClass(this.tooltipElt, "tooltip");
            this.renderer.appendChild(document.getElementsByTagName('body')[0], this.tooltipElt);
        }
        const tip = this.tooltipElt;
        const pos = this.getPosition(this.ref.nativeElement, tip, this.position);
        for (const prop in pos) {
            this.renderer.setStyle(tip, prop, pos[prop] + 'px');
        }
        this.renderer.addClass(tip, 'shown');
    }

    onMouseLeave(): void {
        const wait = this.tooltipElt &&
            parseFloat(window.getComputedStyle(this.tooltipElt)['transition-duration']);
        this.renderer.removeClass(this.tooltipElt, 'shown');
        setTimeout(this.onTransitionEnd, wait * 1000);
    }

    onTransitionEnd = () => {
        if (this.tooltipElt && !this.tooltipElt.classList.contains('shown')) {
            this.tooltipElt.parentNode.removeChild(this.tooltipElt);
            this.tooltipElt = null;
        }
    }

    private getPosition(elt, tip, pos) {
        const rect = {
            top: elt.getBoundingClientRect().top + window.scrollY,
            left: elt.getBoundingClientRect().left + window.scrollX
        };
        let left, top;

        switch (pos) {

            case 'top':
                top = rect.top - tip.offsetHeight - this.offset;
                left = rect.left + elt.offsetWidth / 2 - tip.offsetWidth / 2;
                break;
            case 'left':
                top = rect.top + elt.offsetHeight / 2 - tip.offsetHeight / 2;
                left = rect.left - tip.offsetWidth - this.offset;
                break;
            case 'right':
                top = rect.top + elt.offsetHeight / 2 - tip.offsetHeight / 2;
                left = rect.left + elt.offsetWidth + this.offset;
                break;
            case 'bottom':
            default:
                top = rect.top + elt.offsetHeight + this.offset;
                left = rect.left + elt.offsetWidth / 2 - tip.offsetWidth / 2;
        }

        if (left < 0 ) {
            left = rect.left + elt.offsetWidth + this.offset;
        }
        if (top < 0) {
            top = rect.top + elt.offsetHeight + this.offset;
        }
        if (left + tip.offsetWidth >= window.pageXOffset + window.innerWidth) {
            left = rect.left - tip.offsetWidth - this.offset;
        }
        if (top - tip.offsetHeight >= window.pageYOffset + window.innerHeight) {
            top = rect.top - tip.offsetHeight - this.offset;
        }

        return {
            top: top < 5 ? 5 : top,
            left: left < 5 ? 5 : left
        };
    }

    ngOnDestroy(): void {
        if (this.tooltipElt) {
            this.tooltipElt.parentNode.removeChild(this.tooltipElt);
            this.tooltipElt = null;
        }
    }

}
