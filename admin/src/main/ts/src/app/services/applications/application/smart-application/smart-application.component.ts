import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { Session } from 'src/app/core/store/mappings/session';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { RoleModel } from 'src/app/core/store/models/role.model';
import { SessionModel } from 'src/app/core/store/models/session.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { routing } from '../../../../core/services/routing.service';
import { ServicesStore } from '../../../services.store';

@Component({
    selector: 'ode-smart-application',
    templateUrl: './smart-application.component.html'
})
export class SmartApplicationComponent extends OdeComponent implements OnInit, OnDestroy {
    public currentTab: 'assignment' | 'massAssignment' = 'assignment';

    
    public assignmentGroupPickerList: GroupModel[];

    constructor(injector: Injector,
                public servicesStore: ServicesStore) {
                    super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.subscriptions.add(this.route.params.subscribe(params => {
            if (params.appId) {
                this.servicesStore.application = this.servicesStore.structure
                    .applications.data.find(a => a.id === params.appId);
            }
        }));

        this.subscriptions.add(this.route.data.subscribe(data => {
            if (data.roles) {
                this.servicesStore.application.roles = data.roles;
                this.servicesStore.application.roles = filterRolesByDistributions(
                    this.servicesStore.application.roles.filter(r => r.transverse == false),
                    this.servicesStore.structure.distributions);
            }
        }));

        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.assignmentGroupPickerList = this.servicesStore.structure.groups.data;
                if (!this.structureHasChildren(this.servicesStore.structure) && this.currentTab === 'massAssignment') {
                    this.currentTab = 'assignment';
                }
            }
        }));
    }

    public onAddAssignment($event: {group: GroupModel, role: RoleModel}) {
        $event.role.addGroup($event.group);
    }

    public onRemoveAssignment($event: {group: GroupModel, role: RoleModel}): void {
        $event.role.removeGroup($event.group);
    }

    public onMassAssignment(): void {
        this.servicesStore.application.syncRoles(this.servicesStore.structure.id)
            .then(async () => {
                let roles: Array<RoleModel> = this.servicesStore.application.roles.filter(r => r.transverse == false);

                const session: Session = await SessionModel.getSession();
                if (session.isADMC()) {
                    this.servicesStore.application.roles = roles;
                } else {
                    this.servicesStore.application.roles = filterRolesByDistributions(roles, this.servicesStore.structure.distributions);
                }
            });
    }

    public structureHasChildren(structure: StructureModel): boolean {
        return structure.children && structure.children.length > 0;
    }
}

export function filterRolesByDistributions(roles: RoleModel[], distributions: string[]): RoleModel[] {
    return roles.filter(role => {
        if (role.distributions.length === 0) {
            return true;
        }
        return distributions.some(distribution => role.distributions.indexOf(distribution) >= 0);
    });
}
