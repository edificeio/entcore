import { Component, Input, Renderer, ElementRef, OnDestroy } from '@angular/core'

@Component({
    selector: '[tooltip]',
    template: `<ng-content></ng-content>`,
    styles: [`
        >>> body > div.tooltip {
            position: absolute;
            z-index: 100;
        }
        >>> body > div.tooltip.shown {
        }
    `],
    host: {
        '(mouseenter)': 'onMouseEnter()',
        '(mouseleave)': 'onMouseLeave()'
    }
})
export class TooltipComponent {

    constructor(private ref: ElementRef,
        private renderer : Renderer){}

    @Input("tooltip") tooltipContents: string
    @Input() position : "top" | "left" | "right" | "bottom" = "bottom"
    @Input() offset: number = 5

    private tooltipElt : HTMLElement

    onMouseEnter() : void {
        let r = this.renderer

        if(!this.tooltipElt){
            let body = document.getElementsByTagName('body')[0]
            this.tooltipElt = r.createElement(body, 'div')
            r.setElementClass(this.tooltipElt, "tooltip", true)
            this.tooltipElt.innerHTML = this.tooltipContents
        }
        let tip = this.tooltipElt
        let pos = this.getPosition(this.ref.nativeElement, tip, this.position)
        for(let prop in pos){
            r.setElementStyle(tip, prop, pos[prop] + 'px')
        }
        r.setElementClass(tip, 'shown', true)
    }

    onMouseLeave() : void {
        let wait = this.tooltipElt &&
            parseFloat(window.getComputedStyle(this.tooltipElt)['transition-duration'])
        this.renderer.setElementClass(this.tooltipElt, 'shown', false)
        setTimeout(this.onTransitionEnd, wait*1000)
    }

    onTransitionEnd = () => {
        if(this.tooltipElt && !this.tooltipElt.classList.contains('shown')){
            this.tooltipElt.parentNode.removeChild(this.tooltipElt)
            this.tooltipElt = null
        }
    }

    private getPosition(elt, tip, pos) {
        let rect = {
            top: elt.getBoundingClientRect().top + window.scrollY,
            left: elt.getBoundingClientRect().left + window.scrollX
        }
        let left, top

        switch(pos){

            case "top":
                top = rect.top - tip.offsetHeight - this.offset
                left = rect.left + elt.offsetWidth / 2 - tip.offsetWidth / 2
                break;
            case "left":
                top = rect.top + elt.offsetHeight / 2 - tip.offsetHeight / 2
                left = rect.left - tip.offsetWidth - this.offset
                break;
            case "right":
                top = rect.top + elt.offsetHeight / 2 - tip.offsetHeight / 2
                left = rect.left + elt.offsetWidth + this.offset
                break;
            case "bottom":
            default:
                top = rect.top + elt.offsetHeight + this.offset
                left = rect.left + elt.offsetWidth / 2 - tip.offsetWidth / 2
        }

        if(left < 0 ) {
            left = rect.left + elt.offsetWidth + this.offset
        }
        if(top < 0) {
            top = rect.top + elt.offsetHeight + this.offset
        }
        if(left + tip.offsetWidth >= window.pageXOffset + window.innerWidth){
            left = rect.left - tip.offsetWidth - this.offset
        }
        if(top - tip.offsetHeight >= window.pageYOffset + window.innerHeight){
            top = rect.top - tip.offsetHeight - this.offset
        }

        return {
            top: top < 5 ? 5 : top,
            left: left < 5 ? 5 : left
        }
    }

    ngOnDestroy() : void {
        if(this.tooltipElt){
            this.tooltipElt.parentNode.removeChild(this.tooltipElt)
            this.tooltipElt = null
        }
    }

}
