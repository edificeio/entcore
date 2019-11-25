import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ConnectorModel, GroupModel, RoleModel} from '../../../../core/store/models';
import {Assignment} from '../../../shared/services-types';

@Component({
    selector: 'connector-assignment',
    templateUrl: './connector-assignment.component.html'
})
export class ConnectorAssignmentComponent {
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
}
