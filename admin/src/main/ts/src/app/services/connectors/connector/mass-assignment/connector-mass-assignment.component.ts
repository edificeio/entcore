import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, EventEmitter, Input, OnInit, Output, Injector } from '@angular/core';
import {Profile, Structure} from '../../../shared/services-types';
import {SelectOption} from 'ngx-ode-ui';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {BundlesService} from 'ngx-ode-sijil';

@Component({
    selector: 'ode-connector-mass-assignment',
    templateUrl: './connector-mass-assignment.component.html',
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
export class ConnectorMassAssignmentComponent extends OdeComponent implements OnInit {

    constructor(injector: Injector, private bundlesService: BundlesService, formBuilder: FormBuilder) {
        super(injector);
        this.massAssignmentForm = formBuilder.group({
            profiles: [[], [Validators.required]]
        });
    }
    @Input()
    public structure: Structure;
    @Input()
    public profiles: Array<Profile>;

    @Output()
    submitAssignment: EventEmitter<Array<Profile>> = new EventEmitter();
    @Output()
    submitUnassignment: EventEmitter<Array<Profile>> = new EventEmitter();

    public profileOptions: Array<SelectOption<Profile>> = [];

    public massAssignmentForm: FormGroup;
    public displayedLightbox: 'assignment' | 'unassignment' | 'none' = 'none';

    public translatedSelectedProfiles: string;
    public profileTrackByFn = (p: Profile) => p;

    ngOnInit(): void {
        super.ngOnInit();
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
