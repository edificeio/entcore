import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Data, Router} from '@angular/router';

import {Subscription} from 'rxjs';
import {Config} from '../resolvers/Config';
import { StructureModel } from '../store/models/structure.model';
import { globalStore } from '../store/global.store';

@Component({
    selector: 'ode-app-nav',
    templateUrl: './nav.component.html',
    styleUrls: ['./nav.component.scss']
})
export class NavComponent implements OnInit, OnDestroy {
    // tslint:disable-next-line:variable-name
    private _currentStructure: StructureModel;
    set currentStructure(struct: StructureModel) {
        this._currentStructure = struct;

        const replacerRegex = /^\/{0,1}admin(\/[^\/]+){0,1}/;
        const newPath = window.location.pathname.replace(replacerRegex, `/admin/${struct.id}`);

        this.router.navigateByUrl(newPath);
    }
    get currentStructure() { return this._currentStructure; }
    openside: boolean;
    structureFilter: string;
    structures: StructureModel[];

    @ViewChild('sidePanelOpener', { static: false }) sidePanelOpener: ElementRef;

    private structureSubscriber: Subscription;
    private routeSubscription: Subscription;

    public config: Config;

    constructor(
        public router: Router,
        private route: ActivatedRoute) {}

    ngOnInit() {
        this.structures = globalStore.structures.asTree();

        if (this.structures.length === 1 && !this.structures[0].children) {
            this.currentStructure = this.structures[0];
        }

        this.structureSubscriber = this.route.children[0].params.subscribe(params => {
            const structureId = params.structureId;
            if (structureId) {
                this.currentStructure = globalStore.structures.data.find(
                    s => s.id === structureId);
            }
        });

        this.routeSubscription = this.route.data.subscribe((data: Data) => {
            this.config = data.config;
        });
    }

    ngOnDestroy() {
        if (this.structureSubscriber) {
            this.structureSubscriber.unsubscribe();
        }
        if (this.routeSubscription) {
            this.routeSubscription.unsubscribe();
        }
    }

    public openReports(): void {
        window.open('/timeline/admin-history', '_blank');
    }
}
