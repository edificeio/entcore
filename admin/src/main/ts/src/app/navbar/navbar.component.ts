import http from 'axios';
import { Component, ElementRef, EventEmitter, Injector, Input, Output, ViewChild } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { removeAccents } from 'ngx-ode-ui';
import { StructureModel } from '../core/store/models/structure.model';

@Component({
    selector: 'ode-navbar',
    templateUrl: './navbar.component.html',
    styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent extends OdeComponent {
    @Input()
    structures: Array<StructureModel>;
    @Input()
    currentStructure: StructureModel;
    @Input()
    hideAdminV1Link: boolean;
    @Input()
    isAdmc: boolean;

    @Output()
    selectStructure: EventEmitter<StructureModel> = new EventEmitter<StructureModel>();

    @ViewChild('sidePanelOpener') 
    sidePanelOpener: ElementRef;

    openside: boolean;
    structureFilter: string = '';

    logoutCallback: string = '/';

    constructor(injector: Injector) {
        super(injector);
        this.getLogoutCallback();
    }

    public structureFilterFunction = (v: StructureModel) => {
        const f = this.structureFilter && removeAccents(this.structureFilter.trim().toLocaleLowerCase());
        return !f || removeAccents(v.name.toLocaleLowerCase()).includes(f) || v.UAI && v.UAI.toLocaleLowerCase().includes(f);
    }

    public handleOnSelectStructure(structure: StructureModel): void {
        this.currentStructure = structure;
        if (!this.currentStructure.children) {
            this.openside = false;
        }
        this.selectStructure.emit(structure);
    }

    // TODO keep this??
    public openReports(): void {
        window.open('/timeline/admin-history', '_blank');
    }

    public getLogoutCallback() {
        http.get(`/theme`).then((res) => {
            this.logoutCallback = res.data.logoutCallback;        
        })
    }
}
