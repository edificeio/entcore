import { OdeComponent } from './../../../core/ode/OdeComponent';
import { Component, EventEmitter, Input, Output, Injector } from '@angular/core';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { RoleModel } from 'src/app/core/store/models/role.model';

@Component({
    selector: 'services-role-attribution',
    templateUrl: './services-role-attribution.component.html'
})
export class ServicesRoleAttributionComponent extends OdeComponent {

    @Input() show: boolean;
    @Input() assignmentGroupPickerList: GroupModel[];
    @Input() sort: string;
    @Input() searchPlaceholder: string;
    @Input() noResultsLabel: string;
    @Input() selectedRole: RoleModel;

    @Output('close') close: EventEmitter<void> = new EventEmitter<void>();
    @Output('add') add: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();
    @Output('inputChange') inputChange: EventEmitter<string> = new EventEmitter<string>();

    constructor(injector: Injector) {
        super(injector);
    }

    filterGroups = (group: GroupModel) => {
        // Do not display groups if they are already linked to the selected role
        if (this.selectedRole) {
            const selectedGroupId: string[] = this.selectedRole.groups.map(g => g.id);
            return !selectedGroupId.find(g => g === group.id);
        }
        return true;
    }
}
