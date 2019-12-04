import { OdeComponent } from './../ode/OdeComponent';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, Injector } from '@angular/core';
import { Data } from '@angular/router';

import { Config } from '../resolvers/Config';
import { StructureModel } from '../store/models/structure.model';
import { globalStore } from '../store/global.store';

@Component({
    selector: 'ode-app-nav',
    templateUrl: './nav.component.html',
    styleUrls: ['./nav.component.scss']
})
export class NavComponent extends OdeComponent implements OnInit, OnDestroy {
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

    public config: Config;

    constructor(injector: Injector) {
        super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
        this.structures = globalStore.structures.asTree();

        if (this.structures.length === 1 && !this.structures[0].children) {
            this.currentStructure = this.structures[0];
        }

        this.subscriptions.add(this.route.children[0].params.subscribe(params => {
            const structureId = params.structureId;
            if (structureId) {
                this.currentStructure = globalStore.structures.data.find(
                    s => s.id === structureId);
            }
        }));

        this.subscriptions.add(this.route.data.subscribe((data: Data) => {
            this.config = data.config;
        }));
    }

    public openReports(): void {
        window.open('/timeline/admin-history', '_blank');
    }
}
