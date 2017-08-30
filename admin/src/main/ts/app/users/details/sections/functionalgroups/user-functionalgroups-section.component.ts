import { Component, OnInit } from '@angular/core'

import { AbstractSection } from '../abstract.section'
import { SpinnerService } from '../../../../core/services'
import { GroupModel } from '../../../../core/store/models'

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
                        (inputChange)="inputFilter = $event"
                        [isDisabled]="disableGroup"
                        (onSelect)="spinner.perform($event.id, user.addFunctionalGroup($event), 0)">
                        <ng-template let-item>
                            <span class="display-name">
                                {{ item?.name }}
                            </span>
                        </ng-template>
                    </list-component>
                </div>
            </light-box>
            
            <ul class="actions-list">
                <li *ngFor="let g of details?.functionalGroups">
                    <span>{{ g.name }}</span>
                    <i  class="fa fa-times action" (click)="spinner.perform(g.id, user.removeFunctionalGroup(g), 0)"
                        [tooltip]="'delete.this.group' | translate"
                        [ngClass]="{ disabled: spinner.isLoading(g.id)}">
                    </i>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure']
})
export class UserFunctionalGroupsSection extends AbstractSection implements OnInit {

    listGroupModel: GroupModel[] = []
    showGroupLightbox: boolean = false

    private _inputFilter = ""
    set inputFilter(filter: string) {
        this._inputFilter = filter
    }
    get inputFilter() {
        return this._inputFilter
    }
    
    constructor(
        public spinner: SpinnerService) {
        super()
    }
    
    ngOnInit() {
        if (this.structure.groups.data && this.structure.groups.data.length > 0) {
            this.listGroupModel = this.structure.groups.data.filter(g => g.type === 'FunctionalGroup')
        }
    }

    filterByInput = (g: {id: string, name: string}) => {
        if (!this.inputFilter) return true
        return `${g.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0
    }

    filterGroups = (g: {id: string, name: string}) => {
        if (this.details.functionalGroups) {
            return !this.details.functionalGroups.find(fg => g.id === fg.id)
        }
        return true;
    }
    
    disableGroup = (g) => {
        return this.spinner.isLoading(g.id)
    }

    protected onUserChange() {}

}