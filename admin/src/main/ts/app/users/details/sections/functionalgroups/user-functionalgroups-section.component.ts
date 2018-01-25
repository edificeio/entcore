import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, Input, OnChanges } from '@angular/core'

import { AbstractSection } from '../abstract.section'
import { GroupModel, UserModel, StructureModel } from '../../../../core/store/models'
import { SpinnerService, NotifyService } from '../../../../core/services'

@Component({
    selector: 'user-functionalgroups-section',
    template: `
        <panel-section section-title="users.details.section.functional.groups" [folded]="true">
            <button (click)="showGroupLightbox = true">
                <s5l>add.group</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <lightbox class="inner-list" [show]="showGroupLightbox" (onClose)="showGroupLightbox = false">
                <div class="padded">
                    <h3><s5l>add.group</s5l></h3>
                    <list class="inner-list"
                        [model]="lightboxFunctionalGroups"
                        [inputFilter]="filterByInput"
                        searchPlaceholder="search.group"
                        sort="name"
                        (inputChange)="inputFilter = $event"
                        [isDisabled]="disableGroup"
                        (onSelect)="addGroup($event)">
                        <ng-template let-item>
                            <span class="display-name">
                                {{ item?.name }}
                            </span>
                        </ng-template>
                    </list>
                </div>
            </lightbox>
            
            <ul class="actions-list">
                <li *ngFor="let g of details?.functionalGroups">
                    <div *ngIf="g.id">
                        <span>{{ g.name }}</span>
                        <i  class="fa fa-times action" (click)="removeGroup(g)"
                            [tooltip]="'delete.this.group' | translate"
                            [ngClass]="{ disabled: spinner.isLoading(g.id)}">
                        </i>
                    </div>
                </li>
            </ul>
        </panel-section>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserFunctionalGroupsSection extends AbstractSection implements OnInit, OnChanges {
    lightboxFunctionalGroups: GroupModel[] = [];

    showGroupLightbox: boolean = false;

    @Input() user: UserModel;
    @Input() structure: StructureModel;

    private _inputFilter = "";
    set inputFilter(filter: string) {
        this._inputFilter = filter;
    }
    get inputFilter() {
        return this._inputFilter;
    }
    
    constructor(
        public spinner: SpinnerService,
        private ns: NotifyService,
        private cdRef: ChangeDetectorRef) {
        super()
    }
    
    ngOnInit() {
        this.updateLightboxFunctionalGroups();
    }

    // Refresh data when structure change
    ngOnChanges() {
        this.updateLightboxFunctionalGroups();
    }
    
    private updateLightboxFunctionalGroups() {
        this.lightboxFunctionalGroups = this.structure.groups.data.filter(
            g => g.type === 'FunctionalGroup' 
                && !this.details.functionalGroups.find(mg => mg.id == g.id)
        );
    }

    filterByInput = (g: {id: string, name: string}) => {
        if (!this.inputFilter) return true
        return `${g.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0
    }
    
    disableGroup = (g) => {
        return this.spinner.isLoading(g.id)
    }

    addGroup = (group) => {
        this.spinner.perform('portal-content', this.user.addFunctionalGroup(group)
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.add.group.content', 
                        parameters: {
                            group:  group.name
                        } 
                    }, 'notify.user.add.group.title');

                this.updateLightboxFunctionalGroups();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.add.group.error.content',
                        parameters: {
                            group:  group.name
                        }
                    }, 'notify.user.add.group.error.title', err);
            })
        );
    }

    removeGroup = (group) => {
        this.spinner.perform('portal-content', this.user.removeFunctionalGroup(group)
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.remove.group.content', 
                        parameters: {
                            group:  group.name
                        } 
                    }, 'notify.user.remove.group.title');

                this.updateLightboxFunctionalGroups();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.remove.group.error.content',
                        parameters: {
                            group:  group.name
                        }
                    }, 'notify.user.remove.group.error.title', err);
            })
        );
    }

    protected onUserChange() {}

}
