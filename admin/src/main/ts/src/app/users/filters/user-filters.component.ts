import { OdeComponent } from './../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, Injector } from '@angular/core';
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
export class UserFiltersComponent extends OdeComponent implements OnInit, OnDestroy {

    dateFilter: string;

    constructor(
        private bundles: BundlesService,
        injector: Injector,
        private usersStore: UsersStore,
        public listFilters: UserlistFiltersService) {
            super(injector);
        }

    translate = (...args) => (this.bundles.translate as any)(...args);

    ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.changeDetector.markForCheck();
            }
        }));
        this.usersStore.user = null;

        this.subscriptions.add(this.route.queryParams.subscribe(params => {
            if (params.duplicates) {
                const filter: UserFilter<string> = this.listFilters.filters.find(f => f.type === 'duplicates');
                if (filter) {
                    filter.outputModel = ['users.duplicated'];
                }
            }
        }));

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

    orderer(a) {
        return a;
    }

    deselect(filter, item) {
        if (filter) {
            filter.outputModel.splice(filter.outputModel.indexOf(item), 1);
            filter.observable.next();
        }
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
        if ( filter && filter.datepicker) {
            this.dateFilter = '';
            if (filter.outputModel.length > 0) {
                filter.outputModel[0].date = undefined;
            }
        }
    }

}
