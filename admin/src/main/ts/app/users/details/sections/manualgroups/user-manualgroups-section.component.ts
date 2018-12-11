import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit } from '@angular/core';

import { GroupModel, StructureModel, UserModel } from '../../../../core/store/models';
import { NotifyService, SpinnerService } from '../../../../core/services';
import { GlobalStore } from '../../../../core/store';
import { AbstractSection } from '../abstract.section';
import { isGroupManageable } from '../isGroupManageable';

@Component({
    selector: 'user-manualgroups-section',
    template: `
        <panel-section section-title="users.details.section.manual.groups" [folded]="true">
            <button (click)="showGroupLightbox = true">
                <s5l>add.group</s5l>
                <i class="fa fa-plus-circle"></i>
            </button>
            <lightbox class="inner-list" [show]="showGroupLightbox" (onClose)="showGroupLightbox = false">
                <div class="padded">
                    <h3>
                        <s5l>add.group</s5l>
                    </h3>
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
                <li *ngFor="let group of filteredGroups">
                    <div *ngIf="group.id">
                        <span>{{ group.name }}</span>
                        <i class="fa fa-times action" (click)="removeGroup(group)"
                           [title]="'delete.this.group' | translate"
                           [ngClass]="{ disabled: spinner.isLoading(group.id)}">
                        </i>
                    </div>
                </li>
            </ul>
        </panel-section>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserManualGroupsSection extends AbstractSection implements OnInit, OnChanges {
    public filteredGroups: GroupModel[] = [];
    public lightboxManualGroups: GroupModel[] = [];
    public inputFilter = '';
    public showGroupLightbox = false;

    @Input()
    public user: UserModel;
    @Input()
    public structure: StructureModel;

    constructor(
        public spinner: SpinnerService,
        private notifyService: NotifyService,
        private changeDetectorRef: ChangeDetectorRef,
        private globalStore: GlobalStore) {
        super();
    }

    ngOnInit() {
        this.updateLightboxManualGroups();
        this.filterManageableGroups();
    }

    ngOnChanges() {
        this.updateLightboxManualGroups();
        this.filterManageableGroups();
    }

    private filterManageableGroups() {
        if (this.details) {
            this.filteredGroups = this.details.manualGroups
                .filter(group => isGroupManageable(group, this.globalStore));
        } else {
            this.filteredGroups = [];
        }
    }

    private updateLightboxManualGroups() {
        this.lightboxManualGroups = this.structure.groups.data.filter(
            group => group.type === 'ManualGroup'
                && !this.details.manualGroups.find(manualGroup => manualGroup.id == group.id));
    }

    filterByInput = (manualGroup: { id: string, name: string }): boolean => {
        if (!this.inputFilter) {
            return true;
        }
        return `${manualGroup.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    };

    disableGroup = (manualGroup) => {
        return this.spinner.isLoading(manualGroup.id);
    };

    addGroup =(group) => {
        return this.spinner.perform('portal-content', this.user.addManualGroup(group)
            .then(() => {
                this.notifyService.success(
                    {
                        key: 'notify.user.add.group.content',
                        parameters: {
                            group: group.name
                        }
                    }, 'notify.user.add.group.title');

                this.updateLightboxManualGroups();
                this.changeDetectorRef.markForCheck();
            })
            .catch(err => {
                this.notifyService.error(
                    {
                        key: 'notify.user.add.group.error.content',
                        parameters: {
                            group: group.name
                        }
                    }, 'notify.user.add.group.error.title', err);
            })
        );
    };

    removeGroup =(group) => {
        return this.spinner.perform('portal-content', this.user.removeManualGroup(group)
            .then(() => {
                this.notifyService.success(
                    {
                        key: 'notify.user.remove.group.content',
                        parameters: {
                            group: group.name
                        }
                    }, 'notify.user.remove.group.title');

                this.updateLightboxManualGroups();
                this.changeDetectorRef.markForCheck();
            })
            .catch(err => {
                this.notifyService.error(
                    {
                        key: 'notify.user.remove.group.error.content',
                        parameters: {
                            group: group.name
                        }
                    }, 'notify.user.remove.group.error.title', err);
            })
        );
    };

    protected onUserChange() {
    }
}
