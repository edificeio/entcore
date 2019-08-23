import { Component, OnInit, ChangeDetectionStrategy, Input, ViewChild, Renderer2, ViewEncapsulation } from '@angular/core'
import { MessageFlashService } from '../message-flash.service';

@Component({
    selector: 'message-flash-preview',
    template: `

    <link rel="stylesheet" #myTheme type="text/css" media="screen" />


    <article class="content-box" style="flex: 1 1; margin: 10px; padding: 10px; height: 98%;">
        <div class="twelve cell">
            <div *ngIf="customColor != undefined"  class="flashmsg" [ngStyle]="{'background-color': customColor}">
                <i class="close-2x right-magnet"></i>
                <div [innerHTML]="text"></div>
            </div>
            <div *ngIf="customColor == undefined" class="flashmsg" [ngClass]="color">
                <i class="close-2x right-magnet"></i>
                <div [innerHTML]="text"></div>
            </div>
        </div>
    </article>
       
    `,
    encapsulation: ViewEncapsulation.Native,
    styles: [
        `.flashmsg {
            min-height: 20px;
            margin-right: 0px !important;
        }`
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashPreviewComponent implements OnInit {

    @Input() text: string;
    @Input() color: string;
    @Input() customColor: string;

    @ViewChild('myTheme') myTheme;

    constructor(public renderer: Renderer2) {}

    ngOnInit() {
        MessageFlashService.getTheme().then(path => {
            this.renderer.setAttribute(this.myTheme.nativeElement, "href", path);
        });
    }

    ngOnDestroy() {}

}
