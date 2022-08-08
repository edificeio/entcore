import { Component, EventEmitter, Injector, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { OdeComponent } from 'ngx-ode-core';
import { BundlesService } from 'ngx-ode-sijil';
import { SelectOption } from 'ngx-ode-ui';
import { MassAssignment, Profile, Role, Structure } from '../../../../_shared/services-types';

@Component({
    selector: 'mass-role-assignment',
    templateUrl: './mass-role-assignment.component.html',
    styleUrls: ['./mass-role-assignment.component.scss']
})
export class MassRoleAssignment extends OdeComponent implements OnInit, OnChanges {

    constructor(injector: Injector, private bundlesService: BundlesService, formBuilder: UntypedFormBuilder) {
        super(injector);
        this.assignmentForm = formBuilder.group({
            roles: [[], [Validators.required]],
            profiles: [[], [Validators.required]]
        });
    }
    @Input()
    public structure: Structure;
    @Input()
    public roles: Array<Role>;
    @Input()
    public profiles: Array<Profile>;
    @Output()
    public submitAssignment: EventEmitter<MassAssignment> = new EventEmitter<MassAssignment>();
    @Output()
    public submitUnassignment: EventEmitter<MassAssignment> = new EventEmitter<MassAssignment>();

    public roleOptions: Array<SelectOption<Role>> = [];
    public profileOptions: Array<SelectOption<Profile>> = [];

    public assignmentForm: UntypedFormGroup;
    public displayedLightbox: 'assignment' | 'unassignment' | 'none' = 'none';

    public translatedSelectedRoles: string;
    public translatedSelectedProfiles: string;
    public roleTrackByFn = (r: Role) => r.id;
    public profileTrackByFn = (p: Profile) => p;

    public ngOnInit(): void {
        super.ngOnInit();
        this.computeRoleOptions();
        this.computeProfileOptions();

        this.assignmentForm.get('roles').valueChanges.subscribe((roles: Array<Role>) => {
            this.translatedSelectedRoles = roles ?
                roles.map(r => r.name)
                    .map(r => this.bundlesService.translate(r))
                    .join(', ')
                : '';
        });
        this.assignmentForm.get('profiles').valueChanges.subscribe((profiles: Array<Profile>) => {
            this.translatedSelectedProfiles = profiles ?
                profiles.map(p => this.bundlesService.translate(p))
                    .join(', ')
                : '';
        });
    }

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        this.computeRoleOptions();
        this.computeProfileOptions();
    }

    private computeRoleOptions() {
        this.roleOptions = this.roles.map(r => ({value: r, label: r.name}));
    }

    private computeProfileOptions() {
        this.profileOptions = this.profiles.map(p => ({value: p, label: p}));
    }

    public assign(roles: Array<Role>, profiles: Array<Profile>): void {
        this.submitAssignment.emit({structure: this.structure, roles, profiles});
    }

    public assignFromForm(form: UntypedFormGroup): void {
        const formValue = form.getRawValue() as { roles: Array<Role>, profiles: Array<Profile> };
        this.assign(formValue.roles, formValue.profiles);
    }

    public unassign(roles: Array<Role>, profiles: Array<Profile>): void {
        this.submitUnassignment.emit({structure: this.structure, roles, profiles});
    }

    public unassignFromForm(form: UntypedFormGroup): void {
        const formValue = form.getRawValue() as { roles: Array<Role>, profiles: Array<Profile> };
        this.unassign(formValue.roles, formValue.profiles);
    }
}
