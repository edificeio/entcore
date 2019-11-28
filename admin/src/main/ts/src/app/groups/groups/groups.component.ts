import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs';

import {routing} from '../../core/services/routing.service';
import {GroupsStore} from '../groups.store';
import {CommunicationRulesService} from '../../communication/communication-rules.service';

@Component({
    selector: 'ode-groups-root',
    templateUrl: './groups.component.html',
    providers: [CommunicationRulesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupsComponent implements OnInit, OnDestroy {

    // Subscribers
    private structureSubscriber: Subscription;

    // Tabs
    tabs = [
        {label: 'ManualGroup', view: 'manualGroup'},
        {label: 'ProfileGroup', view: 'profileGroup'},
        {label: 'FunctionalGroup', view: 'functionalGroup'},
        {label: 'FunctionGroup', view: 'functionGroup'}
    ];

    private routerSubscriber: Subscription;
    private error: Error;

    constructor(
        private route: ActivatedRoute,
        public router: Router,
        private cdRef: ChangeDetectorRef,
        public groupsStore: GroupsStore) {
    }

    ngOnInit(): void {
        // Watch selected structure
        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.groupsStore.structure = data.structure;
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

    createButtonHidden() {
        return !this.router.isActive('/admin/' + this.groupsStore.structure.id + '/groups/manualGroup', false)
            || this.router.isActive('/admin/' + this.groupsStore.structure.id + '/groups/manualGroup/create', true);
    }
}
