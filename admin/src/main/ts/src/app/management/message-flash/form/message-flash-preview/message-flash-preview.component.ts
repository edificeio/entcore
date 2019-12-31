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
}
