import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core'
import { BundlesService } from 'sijil'
import { StructureModel } from '../../../../store'
import { UserlistFiltersService } from '../../../../services'
import { Subscription } from 'rxjs/Subscription'
import { ActivatedRoute, Data } from '@angular/router'
import { UsersStore } from '../../store'

@Component({
    selector: 'user-filters',
    template: `
        <div class="panel-header">
            <i class="fa fa-filter"></i>
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
                        [orderBy]="filter.order || orderer"
                        [filter]="filter.filterProp"
                    ></multi-combo>
                    <div class="multi-combo-companion">
                        <div *ngFor="let item of filter.outputModel" (click)="deselect(filter, item)">
                            <span *ngIf="filter.display">
                                {{ item[filter.display] }}
                            </span>
                            <span *ngIf="!filter.display">
                                {{ item | translate }}
                            </span>
                            <i class="fa fa-trash"></i>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserFilters implements OnInit, OnDestroy {

    constructor(
        private bundles: BundlesService,
        private cdRef: ChangeDetectorRef,
        private route: ActivatedRoute,
        private usersStore: UsersStore,
        private listFilters: UserlistFiltersService){}

    ngOnInit() {
        this.dataSubscriber = this.usersStore.onchange.subscribe(() => {
            this.cdRef.markForCheck()
        })
    }

    ngOnDestroy() {
        this.dataSubscriber.unsubscribe()
    }

    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    private dataSubscriber : Subscription
    private structure: StructureModel = this.usersStore.structure

    private orderer(a){
        return a
    }

    private deselect(filter, item) {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1)
        filter.observable.next()
    }
}