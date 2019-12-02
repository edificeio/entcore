import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit, Injector } from '@angular/core';

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
