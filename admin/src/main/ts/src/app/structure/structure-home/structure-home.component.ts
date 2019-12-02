import { OdeComponent } from './../../core/ode/OdeComponent';
import {StructureModel} from '../../core/store/models/structure.model';
import {ActivatedRoute} from '@angular/router';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, Injector } from '@angular/core';
import {Subscription} from 'rxjs';

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
