import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {routing} from '../../core/services/routing.service';
import {StructureModel} from '../../core/store';

@Component({
    selector: 'ode-management-root',
    templateUrl: './management-root.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ManagementRootComponent implements OnInit, OnDestroy {

     // Subscribers
    private structureSubscriber: Subscription;

    // Tabs
    tabs = [
        { label: 'management.message.flash', view: 'message-flash/list', active: 'message-flash'}
    ];

    private routerSubscriber: Subscription;
    private error: Error;
    private structure: StructureModel;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef) { }

    ngOnInit(): void {
        // Watch selected structure
        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
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

    isActive(path: string): boolean {
        return this.router.isActive('/admin/' + this.structure.id + '/management/' + path, false);
    }

}
