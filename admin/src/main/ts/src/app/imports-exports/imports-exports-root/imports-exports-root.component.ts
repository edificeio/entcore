import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data, NavigationEnd } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from '../../core/services/routing.service';

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
        { label: 'massmail.accounts', view: 'massmail' }, // Meld MassMail into export ?
        {label: 'massmessage.accounts', view: 'massmessage'} // to be enabled and disabled via flag
    ];

    showMassMessage:Boolean = false;

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

        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data && data.config) {
                this.showMassMessage = data.config['mass-messaging-enabled'];
            }
        }));
    }

    onError(error: Error) {
        console.error(error);
    }
}
