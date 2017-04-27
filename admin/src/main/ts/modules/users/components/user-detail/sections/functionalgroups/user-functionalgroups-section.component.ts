import { Component, OnInit } from '@angular/core'

import { AbstractSection } from '../abstract.section'
import { LoadingService } from '../../../../../../services'

import { Group } from '../../../../../../store'

@Component({
    selector: 'user-functionalgroups-section',
    template: `
        <panel-section section-title="users.details.section.functional.groups" [folded]="true">
            <button (click)="showGroupLightbox = true">
                <s5l>add.group</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <light-box class="inner-list" [show]="showGroupLightbox" (onClose)="showGroupLightbox = false">
                <div class="padded">
                    <h3><s5l>add.group</s5l></h3>
                    <list-component class="inner-list"
                        [model]="listGroupModel"
                        [inputFilter]="filterByInput"
                        [filters]="filterGroups"
                        searchPlaceholder="search.group"
                        sort="name"
                        [display]="display"
                        (inputChange)="inputFilter = $event"
                        [isDisabled]="disableGroup"
                        (onSelect)="ls.perform($event.id, details.addFunctionalGroup($event), 0)">
                    </list-component>
                </div>
            </light-box>
            
            <ul class="actions-list">
                <li *ngFor="let g of user?.userDetails?.functionalGroups">
                    <span>{{ g.name }}</span>
                    <i  class="fa fa-times action" (click)="ls.perform(g.id, details.removeFunctionalGroup(g), 0)"
                        [tooltip]="'delete.this.group' | translate"
                        [ngClass]="{ disabled: ls.isLoading(g.id)}">
                    </i>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure']
})
export class UserFunctionalGroupsSection extends AbstractSection implements OnInit {

    private listGroupModel: Group[] = []

    private _inputFilter = ""
    set inputFilter(filter: string) {
        this._inputFilter = filter
    }
    get inputFilter() {
        return this._inputFilter
    }

    constructor(protected ls: LoadingService) {
        super()
    }

    ngOnInit() {
        if (this.structure.groups.data && this.structure.groups.data.length > 0) {
            this.listGroupModel = this.structure.groups.data.filter(g => g.type === 'FunctionalGroup')
        }
    }

    private filterByInput = (g: {id: string, name: string}) => {
        if (!this.inputFilter) return true
        return `${g.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0
    }

    private filterGroups = (g: {id: string, name: string}) => {
        return !this.details.functionalGroups.find(fg => g.id === fg.id)
    }
    
    private disableGroup = (g) => {
        return this.ls.isLoading(g.id)
    }

    private display(g: {id: string, name: string}) {
        return g.name
    }

    protected onUserChange() {}

}