import { OdeComponent } from './../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, Injector } from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {routing} from '../../core/services/routing.service';
import {StructureModel} from '../../core/store/models/structure.model';

@Component({
    selector: 'ode-management-root',
    templateUrl: './management-root.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ManagementRootComponent extends OdeComponent implements OnInit, OnDestroy {

    // Tabs
    tabs = [
        { label: 'management.message.flash', view: 'message-flash/list', active: 'message-flash'}
    ];

    private dError: Error;
    private structure: StructureModel;

    constructor(injector: Injector) { 
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        // Watch selected structure
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.changeDetector.markForCheck();
            }
        }));

        this.subscriptions.add(this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.changeDetector.markForCheck();
            }
        }));
    }

    onError(error: Error) {
        console.error(error);
        this.dError = error;
    }

    isActive(path: string): boolean {
        return this.router.isActive('/admin/' + this.structure.id + '/management/' + path, false);
    }

}
