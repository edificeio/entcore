import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit} from '@angular/core';

import {AbstractSection} from '../abstract.section';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { UserModel } from 'src/app/core/store/models/user.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';

@Component({
    selector: 'ode-user-functionalgroups-section',
    templateUrl: './user-functionalgroups-section.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserFunctionalgroupsSectionComponent extends AbstractSection implements OnInit, OnChanges {
    lightboxFunctionalGroups: GroupModel[] = [];
    filteredGroups: GroupModel[] = [];

    showGroupLightbox = false;

    @Input() user: UserModel;
    @Input() structure: StructureModel;

    public inputFilter = '';

    constructor(
        public spinner: SpinnerService,
        private notifyService: NotifyService,
        private changeDetectorRef: ChangeDetectorRef) {
        super();
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
        this.filteredGroups = !this.details ? [] :
            !this.details.functionalGroups ? [] :
                this.details.functionalGroups
                    .filter(group => !!this.structure.groups.data
                        .find(manageableGroup =>
                            (manageableGroup.type === 'FunctionalGroup') && (manageableGroup.id === group.id)));
    }

    private updateLightboxFunctionalGroups() {
        this.lightboxFunctionalGroups = this.structure.groups.data
            .filter(group => group.type === 'FunctionalGroup'
                && this.details.functionalGroups
                && !this.details.functionalGroups.find(functionalGroup => functionalGroup.id == group.id)
            );
    }

    filterByInput = (group: { id: string, name: string }) => {
        if (!this.inputFilter) {
            return true;
        }
        return `${group.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    }

    disableGroup = (group) => {
        return this.spinner.isLoading(group.id);
    }

    addGroup = (group) => {
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
                this.filterManageableGroups();
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

    removeGroup = (group) => {
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
                this.filterManageableGroups();
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
