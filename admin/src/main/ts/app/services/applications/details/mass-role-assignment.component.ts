import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BundlesService } from 'sijil';
import { MultiSelectOption } from '../../../shared/ux/components/multi-select.component';

export interface Role {
    id: string;
    name: string;
}

export type Profile = 'Teacher' | 'Student' | 'Relative' | 'Guest' | 'Personnel';

export interface Structure {
    id: string;
    name: string;
}

export interface Assignment {
    roles: Array<Role>;
    profiles: Array<Profile>;
    structure: Structure;
}

@Component({
    selector: 'mass-role-assignment',
    template: `
        <form [formGroup]="assignmentForm">
            <div class="form__row">
                <span>{{ 'services.mass-assignment.description' | translate }}</span>
            </div>
            <div class="form__row">
                <multi-select
                        class="lct-roles-select"
                        [label]="'services.mass-assignment.roles-select-label'" formControlName="roles"
                        [options]="roleOptions"
                        [trackByFn]="roleTrackByFn"
                        [preview]="true"></multi-select>
            </div>
            <div class="form__row">
                <multi-select
                        class="lct-profiles-select"
                        [label]="'services.mass-assignment.profiles-select-label'" formControlName="profiles"
                        [options]="profileOptions"
                        [trackByFn]="profileTrackByFn"
                        [preview]="true"></multi-select>
            </div>
            <div class="form__row form__row--last">
                <button type="button" class="lct-assign-button submit" [disabled]="!assignmentForm.valid"
                        (click)="displayedLightbox = 'assignment'">
                    {{ 'services.mass-assignment.assign-button' | translate }}
                </button>
                <button type="button" class="lct-unassign-button submit" [disabled]="!assignmentForm.valid"
                        (click)="displayedLightbox = 'unassignment'">
                    {{ 'services.mass-assignment.unassign-button' | translate }}
                </button>
            </div>
        </form>
        <lightbox-confirm title="services.mass-assignment.confirm.title"
                          [show]="displayedLightbox === 'assignment'"
                          (onCancel)="displayedLightbox = 'none'"
                          (onConfirm)="displayedLightbox = 'none'; assignFromForm(assignmentForm);"
        >
            <span [innerHTML]="'services.mass-assignment.confirm.assignment' | translate: {
                structure: structure.name,
                roles: translatedSelectedRoles,
                profiles: translatedSelectedProfiles
            }"></span>
        </lightbox-confirm>
        <lightbox-confirm title="services.mass-assignment.confirm.title"
                          [show]="displayedLightbox === 'unassignment'"
                          (onCancel)="displayedLightbox = 'none'"
                          (onConfirm)="displayedLightbox = 'none'; unassignFromForm(assignmentForm);"
        >
            <span [innerHTML]="'services.mass-assignment.confirm.unassignment' | translate: {
                structure: structure.name,
                roles: translatedSelectedRoles,
                profiles: translatedSelectedProfiles
            }"></span>
        </lightbox-confirm>`,
    styles: [`
        form {
            padding: 15px;
            margin: 0;
        }`, `
        .submit {
            background-color: #ff8352;
            color: white;
        }`, `
        .submit[disabled] {
            color: grey;
        }`, `
        .submit:hover {
            background-color: #ff5e1f;
        }`, `
        .submit:hover[disabled] {
            background-color: #ff8352;
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
export class MassRoleAssignment implements OnInit, OnChanges {
    @Input()
    public structure: Structure;
    @Input()
    public roles: Array<Role>;
    @Input()
    public profiles: Array<Profile>;
    @Output()
    public submitAssignment: EventEmitter<Assignment> = new EventEmitter<Assignment>();
    @Output()
    public submitUnassignment: EventEmitter<Assignment> = new EventEmitter<Assignment>();

    public roleOptions: Array<MultiSelectOption<Role>> = [];
    public roleTrackByFn = (r: Role) => r.id;
    public profileOptions: Array<MultiSelectOption<Profile>> = [];
    public profileTrackByFn = (p: Profile) => p;

    public assignmentForm: FormGroup;
    public displayedLightbox: 'assignment' | 'unassignment' | 'none' = 'none';

    public translatedSelectedRoles: string;
    public translatedSelectedProfiles: string;

    constructor(private bundlesService: BundlesService, formBuilder: FormBuilder) {
        this.assignmentForm = formBuilder.group({
            roles: [[], [Validators.required]],
            profiles: [[], [Validators.required]]
        });
    }

    public ngOnInit(): void {
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
