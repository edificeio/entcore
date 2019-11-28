import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data} from '@angular/router';
import {Subscription} from 'rxjs';
import {BundlesService} from 'sijil';

import {UsersStore} from '../users.store';
import {UserFilter, UserlistFiltersService} from '../../core/services/userlist.filters.service';
import { routing } from 'src/app/core/services/routing.service';

@Component({
    selector: 'ode-user-filters',
    templateUrl: './user-filters.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserFiltersComponent implements OnInit, OnDestroy {

    private structureSubscriber: Subscription;
    private routeSubscriber: Subscription;
    dateFilter: string;

    constructor(
        private bundles: BundlesService,
        private cdRef: ChangeDetectorRef,
        private route: ActivatedRoute,
        private usersStore: UsersStore,
        public listFilters: UserlistFiltersService) {}

    translate = (...args) => (this.bundles.translate as any)(...args);

    ngOnInit() {
        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.cdRef.markForCheck();
            }
        });
        this.usersStore.user = null;

        this.routeSubscriber = this.route.queryParams.subscribe(params => {
            if (params.duplicates) {
                const filter: UserFilter<string> = this.listFilters.filters.find(f => f.type === 'duplicates');
                if (filter) {
                    filter.outputModel = ['users.duplicated'];
                }
            }
        });

        for (const filter of this.listFilters.filters) {
            if (filter.datepicker) {
                if (filter.comboModel[0].date) {
                    this.updateDate(filter.comboModel[0].date, filter);
                } else if (filter.comboModel[1].date) {
                    this.updateDate(filter.comboModel[1].date, filter);
                }
                break;
            }
        }
    }

    ngOnDestroy() {
        this.structureSubscriber.unsubscribe();
        this.routeSubscriber.unsubscribe();
    }

    orderer(a) {
        return a;
    }

    deselect(filter, item) {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1);
        filter.observable.next();
    }

    updateDate(newDate, filter): void {
        if (newDate || this.dateFilter) {
            this.dateFilter = newDate;
            filter.outputModel[0].date = new Date(Date.parse(this.dateFilter));
        } else {
            this.dateFilter = filter.outputModel[0].date.getFullYear() + '/' +
            (filter.outputModel[0].date.getMonth() + 1) + '/' +
            filter.outputModel[0].date.getDate();
        }
    }

    resetDate(filter) {
        if (filter.datepicker) {
            this.dateFilter = '';
            if (filter.outputModel.length > 0) {
                filter.outputModel[0].date = undefined;
            }
        }
    }

}
