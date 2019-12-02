import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit, Injector } from '@angular/core';
import {Subscription} from 'rxjs';

import {SpinnerService} from '../../../../core/services/spinner.service';

@Component({
    selector: 'ode-spinner-cube',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './spinner-cube.component.html',
    styleUrls: ['./spinner-cube.component.scss']
})
export class SpinnerCubeComponent extends OdeComponent implements OnInit, OnDestroy {

    @Input('waitingFor')
    set loadingProp(prop: string) {
        this._loadingProp = prop;
    }
    get loadingProp() { return this._loadingProp; }
    private _loadingProp: string;


    constructor(
        injector: Injector,
        public spinner: SpinnerService) {
            super(injector);
        }

    ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.spinner.trigger.subscribe(() => {
            this.changeDetector.markForCheck();
        }));
    }

}
