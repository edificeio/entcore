import { StructureModel } from '../../../models'
import { StructureCollection, structureCollection } from '../../../models'
import { ActivatedRoute } from '@angular/router'
import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit,
    Input, OnDestroy } from '@angular/core'
import { Subscription } from 'rxjs'

@Component({
    selector: 'structure-home',
    template: `
        <div>
            <h1><i class="fa fa-cogs"></i><s5l>admin.title</s5l></h1>
            <div class="card-layout">
                <quick-actions-card [structure]="structure"></quick-actions-card>
                <user-search-card [structure]="structure" class="align-start"></user-search-card>
                <structure-card [structure]="structure"></structure-card>
                <imports-exports-card [structure]="structure"></imports-exports-card>
            </div>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class StructureHome implements OnInit, OnDestroy {

    private structures : StructureCollection = structureCollection
    private structure: StructureModel
    private routeSubscriber: Subscription

    constructor(private route: ActivatedRoute, private cdRef: ChangeDetectorRef){}

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