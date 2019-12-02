import { OdeComponent } from './../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, Injector } from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {routing} from '../../core/services/routing.service';

@Component({
    selector: 'ode-imports-exports-root',
    templateUrl: './imports-exports-root.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImportsExportsRootComponent extends OdeComponent implements OnInit, OnDestroy {


    // Tabs
    tabs = [
        { label: 'import.users', view: 'import-csv'},
        { label: 'export.accounts', view: 'export' },
        { label: 'massmail.accounts', view: 'massmail' } // Meld MassMail into export ?
    ];

    private dError: Error;

    constructor(injector: Injector) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        // Watch selected structure
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
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
}
