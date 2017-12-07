import { Component, Input, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core'
import { AbstractControl } from '@angular/forms'
import { Router } from '@angular/router'

import { AbstractSection } from '../abstract.section'
import { SpinnerService, NotifyService } from '../../../../core/services'
import { globalStore, StructureCollection, UserModel } from '../../../../core/store'

@Component({
    selector: 'user-structures-section',
    template: `
        <panel-section section-title="users.details.section.structures" [folded]="true">
            <button (click)="showStructuresLightbox = true">
                <s5l>add.structure</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <lightbox class="inner-list"
                    [show]="showStructuresLightbox" (onClose)="showStructuresLightbox = false">
                <div class="padded">
                    <h3><s5l>add.structure</s5l></h3>
                    <list class="inner-list"
                        [model]="structureCollection.data"
                        [inputFilter]="filterByInput"
                        [filters]="filterStructures"
                        searchPlaceholder="search.structure"
                        sort="name"
                        (inputChange)="inputFilter = $event"
                        [isDisabled]="disableStructure"
                        (onSelect)="addStructure($event)">
                        <ng-template let-item>
                            <span class="display-name">
                                {{ item?.name }}
                            </span>
                        </ng-template>
                    </list>
                </div>
            </lightbox>
            <ul class="actions-list">
                <li *ngFor="let structure of user.visibleStructures()">
                    <span>{{ structure.name }}</span>
                    <i  class="fa fa-times action" (click)="removeStructure(structure)"
                        [tooltip]="'delete.this.structure' | translate"
                        [ngClass]="{ disabled: spinner.isLoading(structure.id)}"></i>
                </li>
                <li *ngFor="let structure of user.invisibleStructures()">
                    <span>{{ structure.name }}</span>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserStructuresSection extends AbstractSection {
    @ViewChild("codeInput")
    codeInput : AbstractControl
    showStructuresLightbox: boolean = false
    structureCollection : StructureCollection = globalStore.structures

    constructor(private router: Router,
        public spinner: SpinnerService,
        private ns: NotifyService,
        protected cdRef: ChangeDetectorRef) {
        super()
    }

    protected onUserChange(){}

    private _inputFilter = ""
    set inputFilter(filter: string) {
        this._inputFilter = filter
    }
    get inputFilter() {
        return this._inputFilter
    }

    disableStructure = (s) => {
        return this.spinner.isLoading(s.id)
    }
    
    filterByInput = (s: {id: string, name: string}) => {
        if(!this.inputFilter) return true
        return `${s.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0
    }
    
    filterStructures = (s: {id: string, name: string}) => {
        return !this.user.structures.find(struct => s.id === struct.id)
    }

    addStructure = (structure) => {
        this.spinner.perform('portal-content', this.user.addStructure(structure.id))
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.add.structure.content', 
                        parameters: {
                            structure:  structure.name
                        } 
                    }, 'notify.user.add.structure.title');
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.add.structure.error.content',
                        parameters: {
                            structure:  structure.name
                        }
                    }, 'notify.user.add.structure.error.title', err);
            });
    }

    removeStructure = (structure) => {
        this.spinner.perform('portal-content', this.user.removeStructure(structure.id))
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.remove.structure.content', 
                        parameters: {
                            structure:  structure.name
                        } 
                    }, 'notify.user.remove.structure.title');
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.remove.structure.error.content',
                        parameters: {
                            structure:  structure.name
                        }
                    }, 'notify.user.remove.structure.error.title', err);
            });
    }
}