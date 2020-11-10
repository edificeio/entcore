import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit} from '@angular/core';

import {AbstractSection} from '../abstract.section';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { UserModel } from 'src/app/core/store/models/user.model';
import { SpinnerService } from 'ngx-ode-ui';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { NotifyService } from 'src/app/core/services/notify.service';
import { Subscription } from 'rxjs';
import { ActivatedRoute, Data } from '@angular/router';

@Component({
    selector: 'ode-user-manualgroups-section',
    templateUrl: './user-manualgroups-section.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserManualgroupsSectionComponent extends AbstractSection implements OnInit, OnChanges, OnDestroy {
    public filteredGroups: GroupModel[] = [];
    public lightboxManualGroups: GroupModel[] = [];
    public inputFilter = '';
    public showGroupLightbox = false;

    @Input()
    public user: UserModel;
    @Input()
    public structure: StructureModel;

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
                this.updateLightboxManualGroups();
                this.filterManageableGroups();
                this.changeDetectorRef.markForCheck();
            }
        });
    }

    ngOnChanges() {
        this.updateLightboxManualGroups();
        this.filterManageableGroups();
    }

    ngOnDestroy() {
        this.routeDatasubscription.unsubscribe();
    }

    private filterManageableGroups() {
        this.filteredGroups = !this.details ? [] :
            !this.details.manualGroups ? [] :
                this.details.manualGroups
                    .filter(group => !!this.structure.groups.data
                        .find(manageableGroup =>
                            (manageableGroup.type === 'ManualGroup') && (manageableGroup.id === group.id)));
    }

    private updateLightboxManualGroups() {
        this.lightboxManualGroups = this.structure.groups.data.filter(group => group.type === 'ManualGroup');
        if (this.details.manualGroups && this.details.manualGroups.length > 0) {
            this.lightboxManualGroups = this.lightboxManualGroups.filter(group => !this.details.manualGroups.find(userManualGroup => userManualGroup.id === group.id));
        }
    }

    filterByInput = (manualGroup: { id: string, name: string }): boolean => {
        if (!this.inputFilter) {
            return true;
        }
        return `${manualGroup.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    }

    disableGroup = (manualGroup) => {
        return this.spinner.isLoading(manualGroup.id);
    }

    addGroup = (group) => {
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
