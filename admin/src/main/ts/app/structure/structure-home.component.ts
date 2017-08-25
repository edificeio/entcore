import { StructureModel } from '../core/store'
import { ActivatedRoute } from '@angular/router'
import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit,
    Input, OnDestroy } from '@angular/core'
import { Subscription } from 'rxjs/Subscription'

@Component({
    selector: 'structure-home',
    template: `
        <div>
            <h1><i class="fa fa-cogs"></i><s5l>admin.title</s5l></h1>
            <div class="card-layout">
                <quick-actions-card></quick-actions-card>
                <user-search-card [structure]="structure" class="align-start"></user-search-card>
                <structure-card></structure-card>
                <imports-exports-card></imports-exports-card>
            </div>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class StructureHomeComponent implements OnInit, OnDestroy {

    private routeSubscriber: Subscription
    structure: StructureModel

    constructor(
        private route: ActivatedRoute,
        private cdRef: ChangeDetectorRef){}

    ngOnInit() {
        this.routeSubscriber = this.route.data.subscribe(data => {
            this.structure = data['structure']
            this.cdRef.markForCheck()
        })
    }

    ngOnDestroy() {
        this.routeSubscriber.unsubscribe()
    }

}