import { Component, EventEmitter, Injector, Input, Output } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { RoleModel } from 'src/app/core/store/models/role.model';

@Component({
    selector: 'ode-services-role',
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
