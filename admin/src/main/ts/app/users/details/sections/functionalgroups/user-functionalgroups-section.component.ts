import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit } from '@angular/core';

import { AbstractSection } from '../abstract.section';
import { GroupModel, StructureModel, UserModel } from '../../../../core/store/models';
import { NotifyService, SpinnerService } from '../../../../core/services';
import { isGroupManageable } from "../isGroupManageable";
import { GlobalStore } from "../../../../core/store";

@Component({
    selector: 'user-functionalgroups-section',
    template: `
        <panel-section section-title="users.details.section.functional.groups" [folded]="true">
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
export class UserFunctionalGroupsSection extends AbstractSection implements OnInit, OnChanges {
    lightboxFunctionalGroups: GroupModel[] = [];
    filteredGroups: GroupModel[] = [];

    showGroupLightbox = false;

    @Input() user: UserModel;
    @Input() structure: StructureModel;

    public inputFilter = "";

    constructor(
        public spinner: SpinnerService,
        private globalStore: GlobalStore,
        private notifyService: NotifyService,
        private changeDetectorRef: ChangeDetectorRef) {
        super()
    }

    ngOnInit() {
        this.updateLightboxFunctionalGroups();
        this.filterManageableGroups();
    }

    // Refresh data when structure change
    ngOnChanges() {
        this.updateLightboxFunctionalGroups();
        this.filterManageableGroups();
    }

    private filterManageableGroups() {
        if (this.details) {
            this.filteredGroups = this.details.functionalGroups
                .filter(group => isGroupManageable(group, this.globalStore));
        } else {
            this.filteredGroups = [];
        }
    }

    private updateLightboxFunctionalGroups() {
        this.lightboxFunctionalGroups = this.structure.groups.data
            .filter(group => group.type === 'FunctionalGroup'
                && !this.details.functionalGroups.find(functionalGroup => functionalGroup.id == group.id)
            );
    }

    filterByInput(group: { id: string, name: string }) {
        if (!this.inputFilter) {
            return true
        }
        return `${group.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    }

    disableGroup(group) {
        return this.spinner.isLoading(group.id);
    }

    addGroup(group) {
        this.spinner.perform('portal-content', this.user.addFunctionalGroup(group)
            .then(() => {
                this.notifyService.success(
                    {
                        key: 'notify.user.add.group.content',
                        parameters: {
                            group: group.name
                        }
                    }, 'notify.user.add.group.title');

                this.updateLightboxFunctionalGroups();
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
    }

    removeGroup(group) {
        this.spinner.perform('portal-content', this.user.removeFunctionalGroup(group)
            .then(() => {
                this.notifyService.success(
                    {
                        key: 'notify.user.remove.group.content',
                        parameters: {
                            group: group.name
                        }
                    }, 'notify.user.remove.group.title');

                this.updateLightboxFunctionalGroups();
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
    }

    protected onUserChange() {
    }
}
