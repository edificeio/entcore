import { Component, ChangeDetectionStrategy, ChangeDetectorRef, Input,
    OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core'
import { Location } from '@angular/common'
import { Router, ActivatedRoute } from '@angular/router'

import { SessionModel, StructureModel, structureCollection } from '../../models'
import { Session } from '../../models/mappings'
import { Subscription } from 'rxjs'

@Component({
    selector: 'admin-portal',
    templateUrl: require('./portal.component.html'),//'/admin/public/templates/admin-root.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class Portal implements OnInit, OnDestroy {

    private session: Session

    private structures: StructureModel[]
    private _currentStructure: StructureModel
    set currentStructure(struct: StructureModel){
        this._currentStructure = struct

        let replacerRegex = /^\/{0,1}admin(\/[^\/]+){0,1}/
        let newPath = window.location.pathname.replace(replacerRegex, `/admin/${struct.id}`)

        this.router.navigateByUrl(newPath)
    }
    get currentStructure(){ return this._currentStructure }

    @ViewChild("sidePanelOpener") private sidePanelOpener: ElementRef

    private structureSubscriber : Subscription

    constructor(private cdRef: ChangeDetectorRef,
        private router: Router,
        private location: Location,
        private route: ActivatedRoute) {}

    ngOnInit() {
        this.structures = structureCollection.asTree()
        SessionModel.getSession().then((session) => { this.session = session })

        this.structureSubscriber = this.route.params.subscribe(params => {
            let structureId = params['structureId']
            if(structureId) {
                this.currentStructure = structureCollection.data.find(s => s.id === structureId)
            }
        })
    }

    ngOnDestroy() {
        if(this.structureSubscriber)
            this.structureSubscriber.unsubscribe()
    }

}
