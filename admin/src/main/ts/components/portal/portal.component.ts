import { Component, ChangeDetectionStrategy, ChangeDetectorRef, Input,
    OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core'
import { Location } from '@angular/common'
import { Router, ActivatedRoute } from '@angular/router'

import { Session, SessionModel, StructureModel, globalStore } from '../../store'
import { Subscription } from 'rxjs/Subscription'

@Component({
    selector: 'admin-portal',
    templateUrl: './portal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class Portal implements OnInit, OnDestroy {

    private session: Session
    private _currentStructure: StructureModel
    set currentStructure(struct: StructureModel){
        this._currentStructure = struct

        let replacerRegex = /^\/{0,1}admin(\/[^\/]+){0,1}/
        let newPath = window.location.pathname.replace(replacerRegex, `/admin/${struct.id}`)

        this.router.navigateByUrl(newPath)
    }
    get currentStructure(){ return this._currentStructure }
    openside: boolean
    structureFilter: String
    structures: StructureModel[]

    @ViewChild("sidePanelOpener") sidePanelOpener: ElementRef

    private structureSubscriber : Subscription

    constructor(
        private cdRef: ChangeDetectorRef,
        private router: Router,
        private location: Location,
        private route: ActivatedRoute) {}

    ngOnInit() {
        this.structures = globalStore.structures.asTree()
        SessionModel.getSession().then((session) => { this.session = session })

        this.structureSubscriber = this.route.children[0].params.subscribe(params => {
            let structureId = params['structureId']
            if(structureId) {
                this.currentStructure = globalStore.structures.data.find(s => s.id === structureId)
            }
        })
    }

    ngOnDestroy() {
        if(this.structureSubscriber)
            this.structureSubscriber.unsubscribe()
    }

}
