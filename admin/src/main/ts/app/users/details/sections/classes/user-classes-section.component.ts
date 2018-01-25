import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core'

import { AbstractSection } from '../abstract.section'
import { SpinnerService, NotifyService } from '../../../../core/services'
import { OnChanges, OnInit } from '@angular/core/src/metadata/lifecycle_hooks';

@Component({
    selector: 'user-classes-section',
    template: `
        <panel-section section-title="users.details.section.classes" [folded]="true">
            <button (click)="showClassesLightbox = true">
                <s5l>add.class</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <lightbox class="inner-list" [show]="showClassesLightbox" 
                (onClose)="showClassesLightbox = false">
                <div class="padded">
                    <h3><s5l>add.class</s5l></h3>
                    <list class="inner-list"
                        [model]="lightboxClasses"
                        [inputFilter]="filterByInput"
                        searchPlaceholder="search.class"
                        sort="name"
                        (inputChange)="inputFilter = $event"
                        [isDisabled]="disableClass"
                        (onSelect)="addClass($event)">
                        <ng-template let-item>
                            <span class="display-name">
                                {{ item?.name }}
                            </span>
                        </ng-template>
                    </list>
                </div>
            </lightbox>
            
            <ul class="actions-list">
                <li *ngFor="let c of user?.classes">
                    <span>{{ c.name }}</span>
                    <i class="fa fa-times action" 
                        (click)="removeClass(c)"
                        [tooltip]="'delete.this.class' | translate"
                        [ngClass]="{ disabled: spinner.isLoading('portal-content')}">
                    </i>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserClassesSection extends AbstractSection implements OnInit, OnChanges {
    lightboxClasses: {id: string, name: string}[] = [];
    
    showClassesLightbox: boolean = false

    private _inputFilter = ""
    set inputFilter(filter: string) {
        this._inputFilter = filter
    }
    get inputFilter() {
        return this._inputFilter
    }

    constructor(
        public spinner: SpinnerService,
        private ns: NotifyService,
        private cdRef: ChangeDetectorRef) {
        super()
    }

    ngOnInit() {
        this.updateLightboxClasses();
    }

    ngOnChanges() {
        this.updateLightboxClasses();
    }

    private updateLightboxClasses() {
        this.lightboxClasses = this.structure.classes.filter(
            c => !this.user.classes.find(uc => uc.id == c.id)
        );
    }

    filterByInput = (c: {id: string, name: string}) => {
        if (!this.inputFilter) return true
        return `${c.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0
    }
    
    disableClass = (c) => {
        return this.spinner.isLoading(c.id)
    }

    addClass = (event) => {
        this.spinner.perform('portal-content', this.user.addClass(event))
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.add.class.content', 
                        parameters: {
                            classe:  event.name
                        } 
                    }, 'notify.user.add.class.title');

                this.updateLightboxClasses();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.add.class.error.content',
                        parameters: {
                            classe:  event.name
                        }
                    }, 'notify.user.add.class.error.title', err);
            });
    }

    removeClass = (classe) => {
        this.spinner.perform('portal-content', this.user.removeClass(classe.id))
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.remove.class.content', 
                        parameters: {
                            classe:  classe.name
                        } 
                    }, 'notify.user.remove.class.title');

                this.updateLightboxClasses();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.remove.class.error.content',
                        parameters: {
                            classe:  classe.name
                        }
                    }, 'notify.user.remove.class.error.title', err);
            });
    }

    protected onUserChange() {}
}