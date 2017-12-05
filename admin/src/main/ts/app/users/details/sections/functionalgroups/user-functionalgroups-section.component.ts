import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, Input, OnChanges } from '@angular/core'

import { AbstractSection } from '../abstract.section'
import { SpinnerService } from '../../../../core/services'
import { GroupModel, UserModel, StructureModel } from '../../../../core/store/models'

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
                        [model]="listGroupModel"
                        [inputFilter]="filterByInput"
                        [filters]="filterGroups"
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
                <li *ngFor="let g of listUserGroup">
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
export class UserFunctionalGroupsSection extends AbstractSection implements OnInit {
    listGroupModel: GroupModel[] = [];
    listUserGroup: GroupModel[];
    showGroupLightbox: boolean = false

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
        private cdRef: ChangeDetectorRef) {
        super();
    }
    
    ngOnInit() {
        this.refreshListGroupModel();
        this.refreshListUserGroup();
    }

    // Hack refresh data when structure change
    ngOnChanges() {
        this.refreshListGroupModel();
        this.refreshListUserGroup();
    }

    private refreshListGroupModel = () => {
        if (this.structure.groups.data && this.structure.groups.data.length > 0) {
            this.listGroupModel = this.structure.groups.data.filter(g => g.type === 'FunctionalGroup');
        }
    }

    private refreshListUserGroup = () => {
        if (this.details.functionalGroups) {
            this.listUserGroup = this.details.functionalGroups.filter(ug => this.listGroupModel.find(g => g.id == ug.id));
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

    addGroup(group: GroupModel) {
        this.spinner.perform('portal-content', this.user.addFunctionalGroup(group))
            .then(() => {
                this.listUserGroup.push(group);
                this.cdRef.markForCheck();
            });
    }

    removeGroup(group: GroupModel) {
        this.spinner.perform('portal-content', this.user.removeFunctionalGroup(group))
            .then(() => {
                this.listUserGroup.splice(this.listUserGroup.indexOf(group), 1);
                this.cdRef.markForCheck();
            });
    }

    protected onUserChange() {}

}