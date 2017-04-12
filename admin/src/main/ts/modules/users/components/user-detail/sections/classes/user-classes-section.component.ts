import { Component } from '@angular/core'

import { AbstractSection } from '../abstract.section'
import { LoadingService } from '../../../../../../services'

@Component({
    selector: 'user-classes-section',
    template: `
        <panel-section section-title="users.details.section.classes" [folded]="true">
            <button (click)="showClassesLightbox = true">
                <s5l>add.class</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <light-box class="inner-list" [show]="showClassesLightbox" (onClose)="showClassesLightbox = false">
                <div class="padded">
                    <h3><s5l>add.class</s5l></h3>
                    <list-component class="inner-list"
                        [model]="structure.classes"
                        [inputFilter]="filterByInput"
                        [filters]="filterClasses"
                        searchPlaceholder="search.class"
                        sort="name"
                        [display]="display"
                        (inputChange)="inputFilter = $event"
                        [isDisabled]="disableClass"
                        (onSelect)="ls.perform($event.id, user.addClass($event), 0)">
                    </list-component>
                </div>
            </light-box>
            
            <ul class="actions-list">
                <li *ngFor="let c of user?.classes">
                    <span>{{ c.name }}</span>
                    <i  class="fa fa-times action" (click)="ls.perform(c.id, user.removeClass(c.id), 0)"
                        [tooltip]="'delete.this.class' | translate"
                        [ngClass]="{ disabled: ls.isLoading(c.id)}">
                    </i>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure']
})
export class UserClassesSection extends AbstractSection {
    
    constructor(protected ls: LoadingService) {
        super()
    }

    private _inputFilter = ""
    set inputFilter(filter: string) {
        this._inputFilter = filter
    }
    get inputFilter() {
        return this._inputFilter
    }

    private filterByInput = (c: {id: string, name: string}) => {
        if (!this.inputFilter) return true
        return `${c.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0
    }

    private filterClasses = (c: {id: string, name: string}) => {
        return !this.user.classes.find(classe => c.id === classe.id)
    }
    
    private disableClass = (c) => {
        return this.ls.isLoading(c.id)
    }

    private display(c: {id: string, name: string}) {
        return c.name
    }

    protected onUserChange() {}
}