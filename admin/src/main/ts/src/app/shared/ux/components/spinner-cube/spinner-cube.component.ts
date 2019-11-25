import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';

import {SpinnerService} from '../../../../core/services';

@Component({
    selector: 'ode-spinner-cube',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './spinner-cube.component.html',
    styleUrls: ['./spinner-cube.component.scss']
})
export class SpinnerCubeComponent implements OnInit, OnDestroy {

    @Input('waitingFor')
    set loadingProp(prop: string) {
        this._loadingProp = prop;
    }
    get loadingProp() { return this._loadingProp; }
    private _loadingProp: string;

    private subscription: Subscription;

    constructor(
        public spinner: SpinnerService,
        private cdRef: ChangeDetectorRef) {}

    ngOnInit() {
        this.subscription = this.spinner.trigger.subscribe(() => {
            this.cdRef.markForCheck();
        });
    }

    ngOnDestroy() {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

}
