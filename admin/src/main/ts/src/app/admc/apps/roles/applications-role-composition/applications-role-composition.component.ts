import { Component, EventEmitter, Injector, Input, Output } from '@angular/core';
import { FormControl } from '@angular/forms';
import { OdeComponent } from 'ngx-ode-core';
import { BundlesService } from 'ngx-ode-sijil';
import { RoleActionModel, RoleModel } from 'src/app/core/store/models/role.model';
import { AdmcAppsRolesService } from '../admc-apps-roles.service';

type ActionChange = {
    role:RoleModel;
    action:RoleActionModel; 
    active:boolean;
}

@Component({
    selector: 'ode-applications-role-composition',
    templateUrl: './applications-role-composition.component.html',
    styleUrls: ['./applications-role-composition.component.scss'],
})
export class ApplicationsRoleCompositionComponent extends OdeComponent {
    @Input() role: RoleModel;
    @Input() actions: Array<RoleActionModel> = [];
    @Input() checkedActions: Array<RoleActionModel> = [];
    @Input() distributions: Array<string> = [];
    @Input() checkedDistributions: Array<string> = [];

    @Output() onEdit: EventEmitter<RoleModel> = new EventEmitter<RoleModel>();
    @Output() onSave: EventEmitter<{role:RoleModel, withDistributions:boolean}> = new EventEmitter<{role:RoleModel, withDistributions:boolean}>();
    @Output() onRemove: EventEmitter<RoleModel> = new EventEmitter<RoleModel>();
    @Output() onActionChange: EventEmitter<ActionChange> = new EventEmitter<ActionChange>();

    nameCtl:FormControl;
    distributionsCtl:FormControl;

    // Whether the component allows in-place edition or not.
    public editMode = false;
    public withDistributions:boolean = false;

    // Show/hide the role deletion confirm lightbox.
    public showDeletionConfirm:boolean = false;

    constructor(
            injector: Injector,
            private roleSvc:AdmcAppsRolesService,
            public bundleSvc:BundlesService
        ) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.nameCtl = new FormControl();
        this.distributionsCtl = new FormControl( {value:this.checkedDistributions, disabled:true} );

        this.editMode = (this.role as any).isNew || false;    // Sorry for the hack, but the data model is not clear.
    }

    /** 
     * <mat-select-trigger> are hidden when no value is selected among the available options.
     * @see https://github.com/angular/components/issues/7758
     * We fallback to using a placeholder in that precise case.
     * The placeholder must be undefined as soon as an option is selected.
     */
    getPlaceholderFor(ctl:FormControl) {
        if( ctl.value.length===0 ) return this.bundleSvc.translate('ux.multiselect.selected-distributions', {nb:ctl.value.length});
    }

    onToggleEdit($event) {
        if( this.editMode ) {
            // Update distributions
            if( this.checkedDistributions.length ) {
                // Do not replace the array, only its values !!!
                this.checkedDistributions.splice(0,this.checkedDistributions.length);
            }
            this.distributionsCtl.value.forEach( element => {
                this.checkedDistributions.push( element );
            });

            // => This toggles a save event.
            this.onSave.emit( {role:this.role, withDistributions:this.withDistributions} );
            this.withDistributions = false;
        } else {
            this.onEdit.emit( this.role );
        }
        this.editMode = !this.editMode;
    }

    onFocus($event:FocusEvent) {
        const input = $event.currentTarget as HTMLInputElement;
        if( input ) {
            input.setSelectionRange(0, input.value.length);
        }
    }

    public check(action:RoleActionModel, active) {
        this.onActionChange.emit({role:this.role, action:action, active:active});
    }

    public isChecked = (action:RoleActionModel) =>  {
        return this.checkedActions.findIndex( a => action.name===a.name )!==-1;
    }

    public areAllChecked = () => {
        return this.checkedActions.length === this.actions.length;
    }

    public toggleAll() {
        let checkAll = (this.checkedActions.length < this.actions.length);
        this.actions.forEach( a => {
            if( (this.isChecked(a) && !checkAll) || (!this.isChecked(a) && checkAll) ) {
                this.check( a, checkAll );
            }
        })
    }

}
