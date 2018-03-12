import { Component, Input, ViewChild, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core'
import { AbstractControl } from '@angular/forms'

import { AbstractSection } from '../abstract.section'
import { SpinnerService, NotifyService } from '../../../../core/services'
import { globalStore, StructureCollection, UserModel, StructureModel } from '../../../../core/store'
import { OnChanges, OnInit } from '@angular/core/src/metadata/lifecycle_hooks';

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
                        [model]="lightboxStructures"
                        [inputFilter]="filterByInput"
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
                    <i class="fa fa-times action" (click)="removeStructure(structure)"
                        [title]="'delete.this.structure' | translate"
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
export class UserStructuresSection extends AbstractSection implements OnInit, OnChanges {
    lightboxStructures : StructureModel[];

    showStructuresLightbox: boolean = false;

    private _inputFilter = "";
    set inputFilter(filter: string) {
        this._inputFilter = filter;
    }
    get inputFilter() {
        return this._inputFilter;
    }

    constructor(public spinner: SpinnerService,
        private ns: NotifyService,
        protected cdRef: ChangeDetectorRef) {
        super();
    }
    
    ngOnInit() {
        this.updateLightboxStructures();
    }
    
    ngOnChanges() {
        this.updateLightboxStructures();
    }

    private updateLightboxStructures() {
        this.lightboxStructures = globalStore.structures.data.filter(
            s => !this.user.structures.find(us => us.id == s.id)
        );
    }

    protected onUserChange(){}

    disableStructure = (s) => {
        return this.spinner.isLoading(s.id)
    }
    
    filterByInput = (s: {id: string, name: string}) => {
        if(!this.inputFilter) return true
        return `${s.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0
    }

    addStructure = (structure) => {
        this.spinner.perform('portal-content', this.user.addStructure(structure.id)
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.add.structure.content', 
                        parameters: {
                            structure:  structure.name
                        } 
                    }, 'notify.user.add.structure.title');
                
                this.updateLightboxStructures();
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
            })
        );
    }

    removeStructure = (structure) => {
        this.spinner.perform('portal-content', this.user.removeStructure(structure.id)
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.remove.structure.content', 
                        parameters: {
                            structure:  structure.name
                        } 
                    }, 'notify.user.remove.structure.title');
                
                this.updateLightboxStructures();
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
            })
        );
    }
}