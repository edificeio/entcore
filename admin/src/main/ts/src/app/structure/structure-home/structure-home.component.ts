import {StructureModel} from '../../core/store';
import {ActivatedRoute} from '@angular/router';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';

@Component({
    selector: 'ode-structure-home',
    templateUrl: './structure-home.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class StructureHomeComponent implements OnInit, OnDestroy {

    private routeSubscriber: Subscription;
    structure: StructureModel;

    constructor(
        private route: ActivatedRoute,
        private cdRef: ChangeDetectorRef) {}

    ngOnInit() {
        this.routeSubscriber = this.route.data.subscribe(data => {
            this.structure = data.structure;
            this.cdRef.markForCheck();
        });
    }

    ngOnDestroy() {
        this.routeSubscriber.unsubscribe();
    }

}
