import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, Input, OnChanges } from '@angular/core'

import { GroupModel, UserModel, StructureModel } from '../../../../core/store/models'
import { SpinnerService, NotifyService } from '../../../../core/services'
import { AbstractSection } from '../abstract.section'

@Component({
    selector: 'user-manualgroups-section',
    template: `
        <panel-section section-title="users.details.section.manual.groups" [folded]="true">
            <button (click)="showGroupLightbox = true">
                <s5l>add.group</s5l><i class="fa fa-plus-circle"></i>
            </button>
            <lightbox class="inner-list" [show]="showGroupLightbox" (onClose)="showGroupLightbox = false">
                <div class="padded">
                    <h3><s5l>add.group</s5l></h3>
                    <list class="inner-list"
                        [model]="lightboxManualGroups"
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
                <li *ngFor="let mg of details.manualGroups">
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
export class UserManualGroupsSection extends AbstractSection implements OnInit, OnChanges {
    lightboxManualGroups: GroupModel[] = [];

    showGroupLightbox: boolean = false;

    @Input() user: UserModel;
    @Input() structure: StructureModel;
    
    constructor(
        public spinner: SpinnerService,
        private ns: NotifyService,
        private cdRef: ChangeDetectorRef) {
        super()
    }
    
    private _inputFilter = "";
    set inputFilter(filter: string) {
        this._inputFilter = filter;
    }
    get inputFilter() {
        return this._inputFilter;
    }

    ngOnInit() {
        this.updateLightboxManualGroups();
    }

    // Refresh data when structure change
    ngOnChanges() {
        this.updateLightboxManualGroups();
    }

    private updateLightboxManualGroups() {
        this.lightboxManualGroups = this.structure.groups.data.filter(
            g => g.type === 'ManualGroup' 
                && !this.details.manualGroups.find(mg => mg.id == g.id));
    }

    filterByInput = (mg: {id: string, name: string}) => {
        if (!this.inputFilter) {
            return true;
        }
        return `${mg.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    }

    disableGroup = (mg) => {
        return this.spinner.isLoading(mg.id);
    }

    addGroup = (group) => {
        return this.spinner.perform('portal-content', this.user.addManualGroup(group)
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.add.group.content', 
                        parameters: {
                            group:  group.name
                        } 
                    }, 'notify.user.add.group.title');

                this.updateLightboxManualGroups();
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
        return this.spinner.perform('portal-content', this.user.removeManualGroup(group)
            .then(() => {
                this.ns.success(
                    { 
                        key: 'notify.user.remove.group.content', 
                        parameters: {
                            group:  group.name
                        } 
                    }, 'notify.user.remove.group.title');
                    
                this.updateLightboxManualGroups();
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
