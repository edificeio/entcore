import { Component, EventEmitter, Injector, Input, Output } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { RoleModel } from 'src/app/core/store/models/role.model';
import { WidgetModel } from 'src/app/core/store/models/widget.model';
import { Assignment } from '../../_shared/services-types';

@Component({
    selector: 'ode-widget-assignment',
    templateUrl: 'widget-assigment.component.html'
})

export class WidgetAssignmentComponent extends OdeComponent {
    @Input()
    widget: WidgetModel;
    @Input()
    assignmentGroupPickerList: Array<GroupModel>;

    @Output()
    mandatoryToggle: EventEmitter<Assignment> = new EventEmitter<Assignment>();
    @Output()
    add: EventEmitter<Assignment> = new EventEmitter();
    @Output()
    remove: EventEmitter<Assignment> = new EventEmitter();

    selectedRole: RoleModel;
    showRoleAttributionLightbox = false;
    
    constructor(injector: Injector) {
        super(injector);
    }

    public openRoleAttributionLightbox(role: RoleModel) {
        this.selectedRole = role;
        this.showRoleAttributionLightbox = true;
    }
}