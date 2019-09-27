import { Component, OnInit, ChangeDetectionStrategy, Input} from '@angular/core'
import { MessageFlashService } from '../message-flash.service';

@Component({
    selector: 'message-flash-preview',
    template: `
    <article class="content-box flashmsg__preview">
        <h3><s5l>flashmsg.preview</s5l> :</h3>
        <div class="twelve cell">
            <div *ngIf="customColor != undefined"  class="flashmsg" [ngStyle]="{'background-color': customColor}">
                <i class="close is-pulled-right"></i>
                <div [innerHTML]="text"></div>
            </div>
            <div *ngIf="customColor == undefined" class="flashmsg" [ngClass]="color">
                <i class="close is-pulled-right"></i>
                <div [innerHTML]="text"></div>
            </div>
        </div>
    </article>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashPreviewComponent implements OnInit {

    @Input() text: string;
    @Input() color: string;
    @Input() customColor: string;

    constructor() {}

    ngOnInit() {}

    ngOnDestroy() {}

}
