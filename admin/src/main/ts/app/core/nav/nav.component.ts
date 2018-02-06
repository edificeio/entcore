import { Component, ChangeDetectionStrategy, ChangeDetectorRef, Input,
    OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core'
import { Location } from '@angular/common'
import { Router, ActivatedRoute } from '@angular/router'

import { Session, SessionModel, StructureModel, globalStore } from '../store'
import { Subscription } from 'rxjs/Subscription'

@Component({
    selector: 'app-nav',
    template: `
        <portal>
            <div header-left>
                <i class="fa" aria-hidden="true"
                    [ngClass]="{'fa-times': openside, 'fa-bars': !openside, 'is-hidden': structures.length == 1}"
                    (click)="openside = !openside"
                    #sidePanelOpener></i>
                <span class="link" [routerLink]="'/admin/' + currentStructure?.id">
                    {{ currentStructure?.name }}
                </span>
            </div>
            <div header-right>
                <a class="old-console" href="/directory/admin-console"
                    [tooltip]="'switch.old.admin.console.tooltip' | translate">
                    <i class="fa fa-step-backward"></i>
                </a>
                <a href="/auth/logout" [tooltip]="'logout' | translate">
                    <i class="fa fa-power-off" aria-hidden="true"></i>
                </a>
                <i class="fa fa-exclamation-triangle"
                    *ngIf="currentStructure"
                    title="En construction"
                    disabled></i>
                <i class="fa fa-exchange"
                    *ngIf="currentStructure"
                    [tooltip]="'imports.exports' | translate"
                    [routerLink]="'/admin/' + currentStructure?.id + '/imports-exports/export'"
                    [class.active]="router.isActive('/admin/' + currentStructure?.id + '/imports-exports', false)"></i>
                <i class="fa fa-th"
                    *ngIf="currentStructure"
                    [tooltip]="'services' | translate"
                    [routerLink]="'/admin/' + currentStructure?.id + '/services/applications'"
                    [class.active]="router.isActive('/admin/' + currentStructure?.id + '/services', false)"></i>
                <i class="fa fa-users"
                    *ngIf="currentStructure"
                    [tooltip]="'groups' | translate"
                    [routerLink]="'/admin/' + currentStructure?.id + '/groups/manual'"
                    [class.active]="router.isActive('/admin/' + currentStructure?.id + '/groups', false)"></i>
                <i class="fa fa-user" aria-hidden="true"
                    *ngIf="currentStructure"
                    [tooltip]="'users' | translate"
                    [routerLink]="'/admin/' + currentStructure?.id + '/users/filter'"
                    [class.active]="router.isActive('/admin/' + currentStructure?.id + '/users', false)"></i>
                <i class="fa fa-home" aria-hidden="true"
                    *ngIf="currentStructure"
                    [tooltip]="'nav.structure' | translate"
                    [routerLink]="'/admin/' + currentStructure?.id"
                    [class.active]="router.isActive('/admin/' + currentStructure?.id, true)"></i>
            </div>
            <div section>
                <side-panel 
                    *ngIf="structures.length > 1"
                    [toggle]="openside" 
                    (onClose)="openside = false" 
                    [opener]="sidePanelOpener">
                    <div class="side-search">
                        <search-input (onChange)="structureFilter = $event" 
                            [attr.placeholder]="'search.structure' | translate">
                        </search-input>
                    </div>
                    <item-tree
                        [items]="structures"
                        order="name"
                        display="name"
                        [flatten]="structureFilter && structureFilter.trim() ? ['children'] : []"
                        [filter]="{ name : structureFilter?.trim() }"
                        (onSelect)="currentStructure = $event"
                        [lastSelected]="currentStructure"></item-tree>
                </side-panel>

                <spinner-cube waitingFor="portal-content" class="portal-spinner">
                </spinner-cube>
                
                <div class="welcome-message" *ngIf="router.url === '/admin'">
                    <s5l>message.new.console</s5l>
                    <a class="old-console" href="/directory/admin-console"
                        [tooltip]="'switch.old.admin.console.tooltip' | translate">
                        <i class="fa fa-step-backward"></i>
                    </a>
                </div>

                <div class="portal-body">
                    <router-outlet></router-outlet>
                </div>
            </div>
        </portal>`,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavComponent implements OnInit, OnDestroy {

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
        public router: Router,
        private location: Location,
        private route: ActivatedRoute) {}

    ngOnInit() {
        this.session = this.route.snapshot.data['session']
        this.structures = globalStore.structures.asTree()

        if (this.structures.length == 1)
            this.currentStructure = this.structures[0];

        this.structureSubscriber = this.route.children[0].params.subscribe(params => {
            let structureId = params['structureId']
            if(structureId) {
                this.currentStructure = globalStore.structures.data.find(
                    s => s.id === structureId)
                this.cdRef.markForCheck()
            }
        })
    }

    ngOnDestroy() {
        if(this.structureSubscriber)
            this.structureSubscriber.unsubscribe()
    }
}
