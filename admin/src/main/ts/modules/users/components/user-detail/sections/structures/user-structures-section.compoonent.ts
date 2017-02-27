import { Component, Input, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core'
import { AbstractControl } from '@angular/forms'
import { Router } from '@angular/router'

import { AbstractSection } from '../abstract.section'
import { LoadingService, UserListService } from '../../../../../../services'
import { User } from '../../../../../../store/mappings/user'
import { structureCollection, StructureCollection } from '../../../../../../store'

@Component({
    selector: 'user-structures-section',
    template: `
        <panel-section section-title="users.details.section.structures" [folded]="true">
            <button (click)="showStructuresLightbox = true">
                <s5l>add.structure</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <light-box class="inner-list"
                    [show]="showStructuresLightbox" (onClose)="showStructuresLightbox = false">
                <div class="padded">
                    <h3><s5l>add.structure</s5l></h3>
                    <list-component class="inner-list"
                        [model]="structureCollection.data"
                        [inputFilter]="filterByInput"
                        [filters]="filterStructures"
                        searchPlaceholder="search.structure"
                        sort="name"
                        [display]="display"
                        (inputChange)="inputFilter = $event"
                        [isDisabled]="disableStructure"
                        (onSelect)="wrap(user?.addStructure, $event.id, 0, $event.id)">
                    </list-component>
                </div>
            </light-box>
            <ul class="actions-list">
                <li *ngFor="let structure of user.visibleStructures()">
                    <a class="action" [routerLink]="['/admin', structure.id, 'users', user.id]">
                        {{ structure.name }}
                    </a>
                    <i  class="fa fa-times action" (click)="wrap(user?.removeStructure, structure.id, 0, structure.id)"
                        [tooltip]="'delete.this.structure' | translate"
                        [ngClass]="{ disabled: ls.isLoading(structure.id)}"></i>
                </li>
                <li *ngFor="let structure of user.invisibleStructures()">
                    <span>{{ structure.name }}</span>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure']
})
export class UserStructuresSection extends AbstractSection {

    constructor(private userListService: UserListService,
            private router: Router,
            protected ls: LoadingService,
            protected cdRef: ChangeDetectorRef) {
        super(ls, cdRef)
    }

    private structureCollection : StructureCollection = structureCollection

    @ViewChild("codeInput") codeInput : AbstractControl

    protected onUserChange(){}

    private disableStructure = (s) => {
        return this.ls.isLoading(s.id)
    }

    private isVisibleStructure = (s) => {
        return this.structureCollection.data.find(struct => struct.id === s)
    }

    // Filters
    private _inputFilter = ""
    set inputFilter(filter: string) {
        this._inputFilter = filter
    }
    get inputFilter() {
        return this._inputFilter
    }
    private filterByInput = (s: {id: string, name: string}) => {
        if(!this.inputFilter) return true
        return `${s.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0
    }
    private filterStructures = (s: {id: string, name: string}) => {
        return !this.user.structures.find(struct => s.id === struct.id)
    }

    // Display
    private display = (s: {id: string, name: string}): string => {
        return s.name
    }

    // Loading wrapper
    protected wrap = (func, label, delay = 0, ...args) => {
        return this.ls.wrap(func, label, {delay: delay, cdRef: this.cdRef, binding: this.user}, ...args)
    }

    //Routing
    private routeToStructure(structureId: string) {
        this.router.navigate(['/admin', structureId, 'users', this.user.id])
    }

}