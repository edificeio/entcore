import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, Input, OnChanges } from '@angular/core'

import { AbstractSection } from '../abstract.section'
import { SpinnerService } from '../../../../core/services'
import { GroupModel, UserModel, StructureModel } from '../../../../core/store/models'

@Component({
    selector: 'user-manualgroups-section',
    template: `
        <panel-section section-title="users.details.section.manual.groups" [folded]="true">
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
                        (onSelect)="addGroup($event)">
                        <ng-template let-item>
                            <span class="display-name">
                                {{ item?.name }}
                            </span>
                        </ng-template>
                    </list-component>
                </div>
            </light-box>
    
            <ul class="actions-list">
                <li *ngFor="let mg of listUserGroup">
                    <div *ngIf="mg.id">
                        <span>{{ mg.name }}</span>
                        <i  class="fa fa-times action" (click)="removeGroup(mg)"
                            [tooltip]="'delete.this.group' | translate"
                            [ngClass]="{ disabled: spinner.isLoading(mg.id)}">
                        </i>
                    </div>
                </li>
            </ul>
        </panel-section>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserManualGroupsSection extends AbstractSection implements OnInit {
    listGroupModel: GroupModel[] = [];
    listUserGroup: GroupModel[];
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
            this.listGroupModel = this.structure.groups.data.filter(g => g.type === 'ManualGroup');
        }
    }

    private refreshListUserGroup = () => {
        if (this.details.manualGroups) {
            this.listUserGroup = this.details.manualGroups.filter(ug => this.listGroupModel.find(g => g.id == ug.id));
        }
    }

    filterByInput = (mg: {id: string, name: string}) => {
        if (!this.inputFilter) {
            return true;
        }
        return `${mg.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    }

    filterGroups = (mg: {id: string, name: string}) => {
        if (this.details.manualGroups) {
            return !this.details.manualGroups.find(manualGroup => mg.id === manualGroup.id);
        }
        return true;
    }
    
    disableGroup = (mg) => {
        return this.spinner.isLoading(mg.id);
    }

    addGroup(group: GroupModel) {
        this.spinner.perform('portal-content', this.user.addManualGroup(group))
            .then(() => {
                this.listUserGroup.push(group);
                this.cdRef.markForCheck();
            });
    }

    removeGroup(group: GroupModel) {
        this.spinner.perform('portal-content', this.user.removeManualGroup(group))
            .then(() => {
                this.listUserGroup.splice(this.listUserGroup.indexOf(group), 1);
                this.cdRef.markForCheck();
            });
    }

    protected onUserChange() {}
}