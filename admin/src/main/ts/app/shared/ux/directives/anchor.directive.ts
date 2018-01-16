import { Directive, Input, HostListener } from '@angular/core'

@Directive({ selector: '[anchor]' })
export class AnchorDirective {

    @Input('anchor') anchor : string
    @Input('offset') offset : number = 50

    @HostListener('click', ['$event.target'])
    onClick() {
        let element = document.getElementById(this.anchor)
        window.scrollBy(0, element.getBoundingClientRect().top - this.offset)
    }

}