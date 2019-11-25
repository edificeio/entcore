import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {routing} from '../../core/services/routing.service';

@Component({
    selector: 'ode-imports-exports-root',
    templateUrl: './imports-exports-root.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImportsExportsRootComponent implements OnInit, OnDestroy {

     // Subscriberts
    private structureSubscriber: Subscription;

    // Tabs
    tabs = [
        { label: 'import.users', view: 'import-csv'},
        { label: 'export.accounts', view: 'export' },
        { label: 'massmail.accounts', view: 'massmail' } // Meld MassMail into export ?
    ];

    private routerSubscriber: Subscription;
    private error: Error;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef) { }

    ngOnInit(): void {
        // Watch selected structure
        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.cdRef.markForCheck();
            }
        });

        this.routerSubscriber = this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        });
    }

    ngOnDestroy(): void {
        this.structureSubscriber.unsubscribe();
        this.routerSubscriber.unsubscribe();
    }

    onError(error: Error) {
        console.error(error);
        this.error = error;
    }
}
