import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, EventEmitter, Injector, OnDestroy, OnInit, Output } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { Session } from 'src/app/core/store/mappings/session';
import { SessionModel } from 'src/app/core/store/models/session.model';
import { NotifyService } from '../../../../../core/services/notify.service';
import { RoleModel } from '../../../../../core/store/models/role.model';
import { ServicesStore } from '../../../../services.store';
import { MassAssignment, Profile, Role, Structure } from '../../../../_shared/services-types';
import { filterRolesByDistributions } from '../../smart-application/smart-application.component';

@Component({
    selector: 'ode-smart-mass-role-assignment',
    templateUrl: './smart-mass-role-assignment.component.html'
})
export class SmartMassRoleAssignmentComponent extends OdeComponent implements OnInit, OnDestroy {
    public structure: Structure;
    public profiles: Array<Profile> = ['Guest', 'Personnel', 'Relative', 'Student', 'Teacher', 'AdminLocal'];
    public roles: Array<Role> = [];

    @Output()
    public massAssignment: EventEmitter<void> = new EventEmitter<void>();

    constructor(injector: Injector,
                private http: HttpClient,
                private servicesStore: ServicesStore,
                private notifyService: NotifyService) {
                    super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.structure = {id: this.servicesStore.structure.id, name: this.servicesStore.structure.name};
        this.subscriptions.add(this.route.data.subscribe(async (data: {roles: Array<RoleModel>}) => {
            if (data.roles) {
                let roles: Array<RoleModel> = data.roles.filter(r => r.transverse == false);

                const session: Session = await SessionModel.getSession();
                if (session.isADMC()) {
                    this.roles = roles;
                } else {
                    this.roles = filterRolesByDistributions(roles, this.servicesStore.structure.distributions)
                        .map((r: RoleModel) => ({id: r.id, name: r.name}));
                }

            }
        }));
    }

    public assign(assignment: MassAssignment): void {
        this.http.put<void>(`/appregistry/structures/${assignment.structure.id}/roles`, {
            roles: assignment.roles.map(role => role.id),
            profiles: assignment.profiles
        }).subscribe(
            () => {
                this.notifyService.success(
                    'services.mass-assignment.assign-success.content',
                    'services.mass-assignment.assign-success.title'
                );
                this.massAssignment.emit();
            },
            () => {
                this.notifyService.error(
                    'services.mass-assignment.assign-error.title',
                    'services.mass-assignment.assign-error.title');
            });
    }

    public unassign(assignment: MassAssignment): void {
        const responseType: 'text' = 'text';
        const options = {
            responseType,
            headers: new HttpHeaders({
                'Content-Type': 'application/json',
            }),
            body: {
                roles: assignment.roles.map(role => role.id),
                profiles: assignment.profiles
            }
        };
        this.http.delete(`/appregistry/structures/${assignment.structure.id}/roles`, options).subscribe(
            () => {
                this.notifyService.success(
                    'services.mass-assignment.unassign-success.content',
                    'services.mass-assignment.unassign-success.title'
                );
                this.massAssignment.emit();
            },
            () => {
                this.notifyService.error(
                    'services.mass-assignment.unassign-error.title',
                    'services.mass-assignment.unassign-error.title');
            });
    }
}
