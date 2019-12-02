import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { Component, EventEmitter, Input, Output, Injector } from '@angular/core';
import {Assignment} from '../../../shared/services-types';
import { ConnectorModel } from 'src/app/core/store/models/connector.model';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { RoleModel } from 'src/app/core/store/models/role.model';

@Component({
    selector: 'connector-assignment',
    templateUrl: './connector-assignment.component.html'
})
export class ConnectorAssignmentComponent extends OdeComponent {
    @Input()
    connector: ConnectorModel;
    @Input()
    assignmentGroupPickerList: GroupModel[];
    @Input()
    disabled: boolean;

    @Output()
    remove: EventEmitter<Assignment> = new EventEmitter();
    @Output()
    add: EventEmitter<Assignment> = new EventEmitter();

    selectedRole: RoleModel;
    showRoleAttributionLightbox = false;

    public openRoleAttributionLightbox(role: RoleModel) {
        this.selectedRole = role;
        this.showRoleAttributionLightbox = true;
    }
    constructor(injector: Injector) {
        super(injector);
    }
}
