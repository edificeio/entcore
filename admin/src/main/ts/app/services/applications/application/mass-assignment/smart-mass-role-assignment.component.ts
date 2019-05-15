import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { MassAssignment, Profile, Role, Structure } from '../../../shared/assignment-types';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { RoleModel } from '../../../../core/store/models';
import { ServicesStore } from '../../../services.store';
import { NotifyService } from '../../../../core/services';

@Component({
    selector: 'smart-mass-role-assignment',
    template: `
        <mass-role-assignment
                [structure]="structure"
                [profiles]="profiles"
                [roles]="roles"
                (submitAssignment)="assign($event)"
                (submitUnassignment)="unassign($event)"
        ></mass-role-assignment>`
})
export class SmartMassRoleAssignment implements OnInit, OnDestroy {
    public structure: Structure;
    public profiles: Array<Profile> = ['Guest', 'Personnel', 'Relative', 'Student', 'Teacher'];
    public roles: Array<Role> = [];
    private routeDataSubscription: Subscription;

    @Output()
    public massAssignment: EventEmitter<void> = new EventEmitter<void>();

    constructor(private http: HttpClient, private route: ActivatedRoute, private servicesStore: ServicesStore, private notifyService: NotifyService) {
    }

    ngOnInit(): void {
        this.structure = {id: this.servicesStore.structure.id, name: this.servicesStore.structure.name};
        this.routeDataSubscription = this.route.data.subscribe(data => {
            if (data['roles']) {
                this.roles = data['roles']
                    .filter((r: RoleModel) => r.transverse == false)
                    .map((r: RoleModel) => ({id: r.id, name: r.name}));
            }
        });
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
                () => this.notifyService.error(
                    'services.mass-assignment.assign-error.title',
                    'services.mass-assignment.assign-error.title'
                )
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
                () => this.notifyService.error(
                    'services.mass-assignment.unassign-error.title',
                    'services.mass-assignment.unassign-error.title'
                )
            });
    }

    ngOnDestroy(): void {
        this.routeDataSubscription.unsubscribe();
    }
}
