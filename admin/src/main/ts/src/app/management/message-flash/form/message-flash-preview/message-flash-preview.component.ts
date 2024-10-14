import { ChangeDetectionStrategy, Component, Injector, Input, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-message-flash-preview',
    templateUrl: './message-flash-preview.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashPreviewComponent extends OdeComponent implements OnInit, OnDestroy {

    @Input() text: string;
    @Input() color: string;
    @Input() customColor: string;
    @Input() signature: string;
    @Input() signatureColor: string;

    computedText: string;

    constructor(injector: Injector,
        public domSanitizer: DomSanitizer) {
        super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
        // prevents domSanitizer to display "undefined"
        if (!this.text) {
            this.text = '';
        }
    }

    ngOnChanges(): void {
        this.computedText = (this.text as any).replaceAll(/(<div>[\s\u200B]*<\/div>){2,}/g, '<div>\u200B</div>'); // This code merges consecutive empty lines from adminV1 editor
        this.computedText = (this.text as any).replaceAll(/(<div>([\s\u200B]|<br\/?>)*<\/div>)$/g, ''); // This code remove last empty line from adminV1 editor
        this.computedText = (this.text as any).replaceAll(/(<p><br><\/p>)+/g, ''); // This code merges consecutive empty lines from adminV2 editor;
    }
}
