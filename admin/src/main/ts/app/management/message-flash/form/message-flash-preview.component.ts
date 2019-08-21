import { Component, OnInit, ChangeDetectionStrategy, Input } from '@angular/core'
import { MessageFlashService } from '../message-flash.service';

@Component({
    selector: 'message-flash-preview',
    template: `
        <html>
            
            <body>
                    <div class="flashmsg"
                        [ngStyle]="{'background-color': color }">
                        <div [innerHTML]="text"></div>
                    </div>
            </body>
        </html>
    `,
    styles: [
        `.flashmsg {
            min-height: 20px;
            margin-right: 0px !important;
            background-color: gray;
        }`
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashPreviewComponent implements OnInit{
    
    @Input() text: string;
    @Input() color: string;

    themePath: string;
    
    constructor() {}

    ngOnInit() {
        MessageFlashService.getTheme().then(path => this.themePath = path);
    }

    ngOnDestroy() {}

}
