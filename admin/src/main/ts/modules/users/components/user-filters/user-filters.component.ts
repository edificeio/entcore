import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit } from '@angular/core'
import { ActivatedRoute, Data, Router } from '@angular/router'
import { BundlesService } from 'sijil'
import { UserlistFiltersService } from '../../../../services'
import { Subscription } from 'rxjs/Subscription'
import { UsersStore } from '../../store'
import { routing } from '../../../../routing/routing.utils'

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
export class UserFilters implements OnInit {

    private structureSubscriber : Subscription

    constructor(
        private bundles: BundlesService,
        private cdRef: ChangeDetectorRef,
        private router: Router,
        private route: ActivatedRoute,
        private usersStore: UsersStore,
        private listFilters: UserlistFiltersService){
            console.log(listFilters)
        }

    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    ngOnInit() {
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure']) {
                this.cdRef.markForCheck()
            }
        })
    }

    private orderer(a){
        return a
    }

    private deselect(filter, item) {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1)
        filter.observable.next()
    }
    
}