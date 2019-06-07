import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { Profile, Structure } from '../../../shared/services-types';
import { SelectOption } from '../../../../shared/ux/components/multi-select.component';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { BundlesService } from 'sijil';

@Component({
    selector: 'connector-mass-assignment',
    template: `
        <form [formGroup]="massAssignmentForm">
            <div class="form__row">
                <span>{{ 'services.connector.mass-assignment.description' | translate }}</span>
            </div>
            <div class="form__row">
                <multi-select
                        class="lct-profiles-select"
                        [label]="'services.mass-assignment.profiles-select-label'" 
                        formControlName="profiles"
                        [options]="profileOptions"
                        [trackByFn]="profileTrackByFn"
                        [preview]="true"></multi-select>
            </div>
            <div class="form__row form__row--last">
                <button type="button" class="lct-unassign-button submit" 
                        [disabled]="!massAssignmentForm.valid"
                        (click)="displayedLightbox = 'unassignment'">
                    {{ 'services.mass-assignment.unassign-button' | translate }}
                </button>
                <button type="button" class="lct-assign-button submit" 
                        [disabled]="!massAssignmentForm.valid"
                        (click)="displayedLightbox = 'assignment'">
                    {{ 'services.mass-assignment.assign-button' | translate }}
                </button>
            </div>
        </form>
        <lightbox-confirm lightboxTitle="services.mass-assignment.confirm.title"
            [show]="displayedLightbox === 'assignment'"
            (onCancel)="displayedLightbox = 'none'"
            (onConfirm)="displayedLightbox = 'none'; assignFromForm(massAssignmentForm);">
            <span [innerHTML]="'services.connector.mass-assignment.confirm.assignment' | translate: {
                structure: structure.name,
                profiles: translatedSelectedProfiles
            }"></span>
        </lightbox-confirm>
        <lightbox-confirm lightboxTitle="services.mass-assignment.confirm.title"
                        [show]="displayedLightbox === 'unassignment'"
                        (onCancel)="displayedLightbox = 'none'"
                        (onConfirm)="displayedLightbox = 'none'; unassignFromForm(massAssignmentForm);">
            <span [innerHTML]="'services.connector.mass-assignment.confirm.unassignment' | translate: {
                structure: structure.name,
                profiles: translatedSelectedProfiles
            }"></span>
        </lightbox-confirm>
    `,
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
export class ConnectorMassAssignmentComponent implements OnInit {
    @Input()
    public structure: Structure;
    @Input()
    public profiles: Array<Profile>;

    @Output()
    submitAssignment: EventEmitter<Array<Profile>> = new EventEmitter();
    @Output()
    submitUnassignment: EventEmitter<Array<Profile>> = new EventEmitter();

    public profileOptions: Array<SelectOption<Profile>> = [];
    public profileTrackByFn = (p: Profile) => p;

    public massAssignmentForm: FormGroup;
    public displayedLightbox: 'assignment' | 'unassignment' | 'none' = 'none';

    public translatedSelectedProfiles: string;

    constructor(private bundlesService: BundlesService, formBuilder: FormBuilder) {
        this.massAssignmentForm = formBuilder.group({
            profiles: [[], [Validators.required]]
        });
    }

    ngOnInit(): void {
        this.computeProfileOptions();

        this.massAssignmentForm.get('profiles').valueChanges.subscribe((profiles: Array<Profile>) => {
            this.translatedSelectedProfiles = profiles ?
                profiles.map(p => this.bundlesService.translate(p))
                    .join(', ')
                : '';
        });
    }

    private computeProfileOptions() {
        this.profileOptions = this.profiles.map(p => ({value: p, label: p}));
    }

    public assign(profiles: Array<Profile>): void {
        this.submitAssignment.emit(profiles);
    }

    public assignFromForm(form: FormGroup): void {
        const formValue = form.getRawValue() as { profiles: Array<Profile> };
        this.assign(formValue.profiles);        
    }

    public unassign(profiles: Array<Profile>): void {
        this.submitUnassignment.emit(profiles);
    }

    public unassignFromForm(form: FormGroup): void {
        const formValue = form.getRawValue() as { profiles: Array<Profile> };
        this.unassign(formValue.profiles);
    }
}