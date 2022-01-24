import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';

@Component({
    selector: 'ode-empty-screen',
    templateUrl: './empty-screen.component.html',
    styleUrls: ['./empty-screen.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyScreenComponent extends OdeComponent implements OnInit, OnDestroy {

    constructor(injector: Injector) {
        super(injector);
    }

}
