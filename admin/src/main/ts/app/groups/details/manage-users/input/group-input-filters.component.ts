import { Component, ChangeDetectionStrategy, ChangeDetectorRef, Input, Output
    , OnChanges, EventEmitter, ElementRef } from '@angular/core'
import { ActivatedRoute, Data, Router } from '@angular/router'
import { BundlesService } from 'sijil'

import { StructureModel } from '../../../../core/store/models/structure.model'
import { UserlistFiltersService, ProfilesService } from '../../../../core/services'

@Component({
    selector: 'group-input-filters-users',
    template: `
        <i class="fa fa-filter" (click)="toggleVisibility()" 
            [tooltip]="'filters' | translate"></i>
        <div *ngIf="show">
            <div *ngFor="let filter of listFilters.filters">
                <div *ngIf="filter.comboModel.length > 0">
                    <multi-combo
                        [comboModel]="filter.comboModel"
                        [(outputModel)]="filter.outputModel"
                        [title]="filter.label | translate"
                        [display]="filter.display || translate"
                        [orderBy]="filter.order || orderer"
                        [filter]="filter.filterProp">
                    </multi-combo>
                    <div class="multi-combo-companion">
                        <div *ngFor="let item of filter.outputModel" 
                            (click)="deselect(filter, item)">
                            <span *ngIf="filter.display">
                                {{ item[filter.display] }}
                            </span>
                            <span *ngIf="!filter.display">
                                {{ item | translate }}
                            </span>
                            <i class="fa fa-trash is-size-5"></i>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `,
    host: {
        '(document:click)': 'onClick($event)',
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupInputFilters implements OnChanges {

    @Input()
    private structure: StructureModel
    
    @Output() 
    private onOpen = new EventEmitter<any>()
    
    @Output() 
    private onClose = new EventEmitter<any>()

    show: boolean = false

    private deselectItem: boolean = false

    constructor(
        private _eref: ElementRef,
        private bundles: BundlesService,
        public listFilters: UserlistFiltersService,
        private cdRef: ChangeDetectorRef){}

    ngOnChanges(): void {
        this.initFilters()
    }

    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    private initFilters() {
        this.listFilters.resetFilters()

        this.structure.syncClasses().then(() => {
            this.listFilters.setClassesComboModel(this.structure.classes)
            this.cdRef.markForCheck()
        })
        this.structure.syncAafFunctions().then(() => {
            this.listFilters.setFunctionsComboModel(this.structure.aafFunctions)
            this.cdRef.markForCheck()
        })
        ProfilesService.getProfiles().then(p => {
            this.structure.profiles = p
            this.listFilters.setProfilesComboModel(this.structure.profiles.map(p => p.name))
            this.cdRef.markForCheck()
        })
        this.structure.groups.sync().then(() => {
            this.listFilters.setFunctionalGroupsComboModel(
                this.structure.groups.data.filter(g => g.type === 'FunctionalGroup').map(g => g.name))
            this.listFilters.setManualGroupsComboModel(
                this.structure.groups.data.filter(g => g.type === 'ManualGroup').map(g => g.name))
            this.cdRef.markForCheck()
        })
    }

    orderer(a){
        return a
    }

    deselect(filter, item) {
        filter.outputModel.splice(filter.outputModel.indexOf(item), 1)
        this.deselectItem = true
    }

    onClick(event) {
        if (this.show && !this._eref.nativeElement.contains(event.target) && !this.deselectItem) {
            this.toggleVisibility()
        }
        this.deselectItem = false
        return true
    }

    toggleVisibility(): void {
        this.show = !this.show
    }
}
