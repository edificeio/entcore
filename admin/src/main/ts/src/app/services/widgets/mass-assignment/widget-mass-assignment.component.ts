import { Component, EventEmitter, Injector, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { WidgetModel } from '../../../core/store/models/widget.model';
import { Profile, Structure } from '../../_shared/services-types';
import { SelectOption, SpinnerService } from 'ngx-ode-ui';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { BundlesService } from 'ngx-ode-sijil';
import { WidgetService } from 'src/app/core/services/widgets.service';

@Component({
    selector: 'ode-widget-mass-assignment',
    templateUrl: './widget-mass-assignment.component.html',
    styleUrls: ['./widget-mass-assignment.component.scss']
})
export class WidgetMassAssignmentComponent extends OdeComponent implements OnInit, OnChanges {
    @Input() public structure: Structure;
    @Input() public widget: WidgetModel;
    public profiles: Array<Profile> = ['Guest', 'Personnel', 'Relative', 'Student', 'Teacher', 'AdminLocal'];
    public profileOptions: Array<SelectOption<Profile>> = [];

    public translatedSelectedProfiles: string;
    public profileTrackByFn = (p: Profile) => p;

    public assignmentForm: UntypedFormGroup;
    public displayedLightbox: 'assignment' | 'unassignment' | 'pin' | 'unpin' | 'none' = 'none';

    @Output() public massChange: EventEmitter<void> = new EventEmitter<void>();

    constructor(injector: Injector,
                formBuilder: UntypedFormBuilder,
                private bundlesService: BundlesService,
                private spinner: SpinnerService,
                private widgetService: WidgetService,
            ) {
        super(injector);
        this.assignmentForm = formBuilder.group({
            profiles: [[], [Validators.required]]
        });
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.computeProfileOptions();

        this.assignmentForm.get('profiles').valueChanges.subscribe((profiles: Array<Profile>) => {
            this.translatedSelectedProfiles = profiles ?
                profiles.map(p => this.bundlesService.translate(p))
                    .join(', ')
                : '';
        });
    }

    public ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        this.computeProfileOptions();
    }

    private computeProfileOptions() {
        this.profileOptions = this.profiles.map(p => ({value: p, label: p}));
    }

    public onConfirm() {
        switch( this.displayedLightbox ) {
            case 'assignment':      this.assign(); break;
            case 'unassignment':    this.unassign(); break;
            case 'pin':             this.pin(); break;
            case 'unpin':           this.unpin(); break;
            default: break;
        }
        this.displayedLightbox = 'none';
    }

    protected async assign() {
        const formValue = this.assignmentForm.getRawValue() as { profiles: Array<Profile> };
        await this.spinner.perform(
            'portal-content',
            this.widgetService.massLink( this.widget, this.structure, formValue.profiles )
            .then( () => this.massChange.emit() )
        );
    }

    protected async unassign() {
        const formValue = this.assignmentForm.getRawValue() as { profiles: Array<Profile> };
        await this.spinner.perform(
            'portal-content',
            this.widgetService.massUnlink( this.widget, this.structure, formValue.profiles )
            .then( () => this.massChange.emit() )
        );
    }

    protected async pin() {
        const formValue = this.assignmentForm.getRawValue() as { profiles: Array<Profile> };
        await this.spinner.perform(
            'portal-content',
            this.widgetService.massSetMandatory( this.widget, this.structure, formValue.profiles )
            .then( () => this.massChange.emit() )
        );
    }

    protected async unpin() {
        const formValue = this.assignmentForm.getRawValue() as { profiles: Array<Profile> };
        await this.spinner.perform(
            'portal-content',
            this.widgetService.massUnsetMandatory( this.widget, this.structure, formValue.profiles )
            .then( () => this.massChange.emit() )
        );
    }
}
