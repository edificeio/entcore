import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit, OnDestroy } from '@angular/core'
import { ActivatedRoute, Data } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { BundlesService } from 'sijil'

import { UserlistFiltersService, routing } from '../../core/services'
import { UsersStore } from '../users.store';
import { UserFilter } from '../../core/services/userlist.filters.service';

@Component({
    selector: 'user-filters',
    template: `
        <div class="panel-header">
            <i class="fa fa-filter is-size-3"></i>
            <span><s5l>filters</s5l></span>
        </div>
        <div class="padded">
            <div *ngFor="let filter of listFilters.filters">
                <div *ngIf="filter.comboModel.length > 0">
                    <multi-combo
                        [comboModel]="filter.comboModel"
                        [(outputModel)]="filter.outputModel"
                        [title]="filter.label | translate"
                        [display]="filter.display || translate"
                        [max]="filter.datepicker ? 1 : filter.comboModel.length"
                        [orderBy]="filter.order || orderer"
                        [filter]="filter.filterProp"
                        (outputModelChange)="resetDate(filter)"
                    ></multi-combo>
                    <div class="multi-combo-companion">
                        <div *ngFor="let item of filter.outputModel" 
                            (click)="deselect(filter, item)">
                            <span *ngIf="filter.display">
                                {{ item[filter.display] | translate }}
                            </span>
                            <span *ngIf="!filter.display">
                                {{ item | translate }}
                            </span>
                            <i class="fa fa-trash is-size-5"></i>
                        </div>
                        <div *ngIf="filter.datepicker&&filter.outputModel.length>0">
                            <date-picker [ngModel]="dateFilter" (ngModelChange)="updateDate($event,filter)"></date-picker>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserFilters implements OnInit, OnDestroy {

    private structureSubscriber : Subscription;
    private routeSubscriber: Subscription;
    dateFilter: string

    constructor(
        private bundles: BundlesService,
        private cdRef: ChangeDetectorRef,
        private route: ActivatedRoute,
        private usersStore: UsersStore,
        public listFilters: UserlistFiltersService){}

    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    ngOnInit() {
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure']) {
                this.cdRef.markForCheck()
            }
        });
        this.usersStore.user = null;

        this.routeSubscriber = this.route.queryParams.subscribe(params => {
            if(params['duplicates']) {
                let filter: UserFilter<string> = this.listFilters.filters.find(f => f.type === 'duplicates');
                if (filter) {
                    filter.outputModel = ['users.duplicated'];
                }
            }
        });
    }

    ngOnDestroy() {
        this.structureSubscriber.unsubscribe();
        this.routeSubscriber.unsubscribe();
    }

    private orderer(a){
        return a
    }

    private deselect(filter, item) {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1)
        filter.observable.next()
    }

    updateDate(newDate,filter): void {
        if(newDate || this.dateFilter) {
            this.dateFilter = newDate;
            filter.outputModel[0].date = new Date(Date.parse(this.dateFilter));
        }
        else {
            this.dateFilter = filter.outputModel[0].date.getFullYear() + '/' +
            (filter.outputModel[0].date.getMonth()+1) + '/' +
            filter.outputModel[0].date.getDate()
        }
    }

    resetDate(filter) {
        if(filter.datepicker) {
            this.dateFilter = "";
            if(filter.outputModel.length > 0) {
                filter.outputModel[0].date = undefined;
            }
        }
    }

}
