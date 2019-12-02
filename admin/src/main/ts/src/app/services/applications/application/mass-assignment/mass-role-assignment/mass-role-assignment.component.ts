import { OdeComponent } from './../../../../../core/ode/OdeComponent';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, Injector } from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {BundlesService} from 'sijil';
import {MassAssignment, Profile, Role, Structure} from '../../../../shared/services-types';
import {SelectOption} from '../../../../../shared/ux/components/multi-select/multi-select.component';

@Component({
    selector: 'mass-role-assignment',
    templateUrl: './mass-role-assignment.component.html',
    styles: [`
        form {
            padding: 0 15px 15px 15px;
            margin: 0;
        }`, `
        .submit {
            background-color: #ff8352;
            color: white;
            min-width: 80px;
            text-align: center;
        }`, `
        .submit[disabled], .submit[disabled]:hover {
            background-color: #f2f2f2;
            color: grey;
        }`, `
        .submit:hover {
            background-color: #ff5e1f;
        }`, `
        select {
            display: inline-block;
            vertical-align: middle;
            min-width: 400px;
            height: 105px;
            font-size: 16px;
        }`, `
        .form__row {
            margin: 15px 0;
        }`, `
        .form__row--last {
            margin-bottom: 0;
            text-align: right;
        }`
    ]
})
export class MassRoleAssignment extends OdeComponent implements OnInit, OnChanges {

    constructor(injector: Injector, private bundlesService: BundlesService, formBuilder: FormBuilder) {
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

    public assignmentForm: FormGroup;
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

    public assignFromForm(form: FormGroup): void {
        const formValue = form.getRawValue() as { roles: Array<Role>, profiles: Array<Profile> };
        this.assign(formValue.roles, formValue.profiles);
    }

    public unassign(roles: Array<Role>, profiles: Array<Profile>): void {
        this.submitUnassignment.emit({structure: this.structure, roles, profiles});
    }

    public unassignFromForm(form: FormGroup): void {
        const formValue = form.getRawValue() as { roles: Array<Role>, profiles: Array<Profile> };
        this.unassign(formValue.roles, formValue.profiles);
    }
}
