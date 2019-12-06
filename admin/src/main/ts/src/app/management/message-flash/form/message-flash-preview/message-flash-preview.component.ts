import { ChangeDetectionStrategy, Component, Injector, Input, OnDestroy, OnInit } from '@angular/core';
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

    constructor(injector: Injector) {
        super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
    }

    

}
