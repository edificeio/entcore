import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit} from '@angular/core';

import {AbstractSection} from '../abstract.section';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { UserModel } from 'src/app/core/store/models/user.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { Subscription } from 'rxjs';
import { ActivatedRoute, Data } from '@angular/router';

@Component({
    selector: 'ode-user-functionalgroups-section',
    templateUrl: './user-functionalgroups-section.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserFunctionalgroupsSectionComponent extends AbstractSection implements OnInit, OnChanges, OnDestroy {
    lightboxFunctionalGroups: GroupModel[] = [];
    filteredGroups: GroupModel[] = [];

    showGroupLightbox = false;

    _inUser: UserModel;
    get inUser() {
      return this._inUser;
    }
    @Input() set inUser(user: UserModel) {
        this._inUser = user;
        this.user = user;
    }

    @Input() structure: StructureModel;

    public inputFilter = '';

    private routeDatasubscription: Subscription;

    constructor(
        public spinner: SpinnerService,
        private notifyService: NotifyService,
        private changeDetectorRef: ChangeDetectorRef,
        private activatedRoute: ActivatedRoute) {
        super();
    }

    ngOnInit() {
        this.routeDatasubscription = this.activatedRoute.data.subscribe((data: Data) => {
            if (data && data.user) {
                this.details = data.user.userDetails;
                this.updateLightboxFunctionalGroups();
                this.filterManageableGroups();
                this.changeDetectorRef.markForCheck();
            }
        });
    }

    // Refresh data when structure change
    ngOnChanges() {
        this.updateLightboxFunctionalGroups();
        this.filterManageableGroups();
    }

    ngOnDestroy() {
        this.routeDatasubscription.unsubscribe();
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
        this.lightboxFunctionalGroups = this.structure.groups.data.filter(group => group.type === 'FunctionalGroup');
        if (this.details.functionalGroups && this.details.functionalGroups.length > 0) {
            this.lightboxFunctionalGroups = this.lightboxFunctionalGroups.filter(group => !this.details.functionalGroups.find(userFunctionalGroup => userFunctionalGroup.id === group.id));
        }
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
