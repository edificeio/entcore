import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { StructureModel } from '../../core/store/models/structure.model';

@Component({
    selector: 'ode-structure-home',
    templateUrl: './structure-home.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class StructureHomeComponent extends OdeComponent implements OnInit, OnDestroy {

    structure: StructureModel;

    constructor(injector: Injector) {
        super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.route.data.subscribe(data => {
            this.structure = data.structure;
            this.changeDetector.markForCheck();
        }));
    }

}
