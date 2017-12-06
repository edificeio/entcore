import { ChangeDetectorRef } from '@angular/core';
import { Component, ViewChild, ElementRef,
    EventEmitter, Renderer, Input, Output } from '@angular/core';

@Component({
    selector: 'lightbox',
    template: `
        <section *ngIf="_show" #section>
            <div overlay #overlay></div>
            <div content>
                <i (click)="close()" *ngIf="showCloseButton == true"></i>
                <ng-content></ng-content>
            </div>
        </section>
    `,
    styles: [`
        section {
            position: fixed;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100%;
            width: 100%;
            z-index: 100;
            top: 0;
            left: 0;
            opacity: 0;
            transition: opacity 0.35s;
        }
        section[hidden] {
            display: none !important;
        }
        :host.shown > section {
            opacity: 1;
        }
        div[overlay] {
            position: absolute;
            top: 0;
            left: 0;
            opacity: 0.7;
            width: 100%;
            height: 100%;
            background-color: black;
        }
        div[content] {
            flex: 0 0 auto;
            min-width: 25%;
            max-width: 75%;
            max-height: 75%;
            overflow: scroll;
            background-color: white;
            z-index: 2;
        }
    `],
    host: {
        '(click)': 'onClick($event)'
    }

})
export class LightBoxComponent {

    constructor(
        private cdRef: ChangeDetectorRef,
        private renderer: Renderer,
        private host: ElementRef) { }

    /* Inputs */

    @Input()
    set show(s: boolean) {
        if(this.timer)
            clearTimeout(this.timer)
        if (s) {
            this._show = true
            this.timer = window.setTimeout(() => {
                this.renderer.setElementClass(this.host.nativeElement, 'shown', true)
                this.timer = null
                this.cdRef.markForCheck()
            }, 100)
        } else {
            let wait = parseFloat(this.section &&
                window.getComputedStyle(this.section.nativeElement)['transition-duration'])
            this.renderer.setElementClass(this.host.nativeElement, 'shown', false)
            this.timer = window.setTimeout(() => {
                this._show = false
                this.timer = null
                this.cdRef.markForCheck()
            }, wait*1000)
        }
    }
    get show(): boolean { 
        return this._show
    }
    _show: boolean = false

    @Input() showCloseButton: boolean = true;

    /* Outputs */
    @Output() onClose = new EventEmitter<any>()

    /* View */
    @ViewChild("section") section : ElementRef
    @ViewChild("overlay") overlay : ElementRef

    /* Internal logic */
    private timer : number

    onClick(event: MouseEvent) {
        if (this.overlay.nativeElement.contains(event.target)) {
            this.close()
        }
    }

    private close() {
        this.show = false
        this.onClose.emit()
    }
}
