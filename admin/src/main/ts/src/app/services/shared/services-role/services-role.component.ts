import { OdeComponent } from './../../../core/ode/OdeComponent';
import { Component, EventEmitter, Input, Output, Injector } from '@angular/core';
import { RoleModel } from 'src/app/core/store/models/role.model';
import { GroupModel } from 'src/app/core/store/models/group.model';

@Component({
    selector: 'services-role',
    templateUrl: './services-role.component.html'
})
export class ServicesRoleComponent extends OdeComponent {
    @Input() role: RoleModel;
    @Input() disabled: boolean;

    @Output() openLightbox: EventEmitter<{}> = new EventEmitter();
    @Output() onRemove: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();

    constructor(injector: Injector) {
        super(injector);
    }
}
