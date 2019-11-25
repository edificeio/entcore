import {ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, Output, Renderer2, ViewChild} from '@angular/core';

@Component({
    selector: 'ode-lightbox',
    templateUrl: './lightbox.component.html',
    styleUrls: ['./lightbox.component.scss'],
    host: {
        '(click)': 'onClick($event)'
    }

})
export class LightBoxComponent {

    constructor(
        private cdRef: ChangeDetectorRef,
        private renderer: Renderer2,
        private host: ElementRef) {
    }

    @Input()
    set show(s: boolean) {
        if (this.timer) {
            clearTimeout(this.timer);
        }
        if (s) {
            this._show = true;
            this.timer = window.setTimeout(() => {
                this.renderer.addClass(this.host.nativeElement, 'shown');
                this.timer = null;
                this.cdRef.markForCheck();
            }, 100);
        } else {
            const wait = parseFloat(this.section &&
                window.getComputedStyle(this.section.nativeElement)['transition-duration']);
            this.renderer.removeClass(this.host.nativeElement, 'shown');
            this.timer = window.setTimeout(() => {
                this._show = false;
                this.timer = null;
                this.cdRef.markForCheck();
            }, wait * 1000);
        }
    }

    get show(): boolean {
        return this._show;
    }

    _show = false;

    @Input() showCloseButton = true;

    @Output() onClose = new EventEmitter<any>();

    @ViewChild('section', { static: false }) section: ElementRef;
    @ViewChild('overlay', { static: false }) overlay: ElementRef;

    private timer: number;

    onClick(event: MouseEvent) {
        if (this.overlay.nativeElement.contains(event.target)) {
            this.close();
        }
    }

    public close() {
        this.show = false;
        this.onClose.emit();
    }
}
