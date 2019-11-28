import {Component, EventEmitter, Input, Output} from '@angular/core';
import { RoleModel } from 'src/app/core/store/models/role.model';
import { GroupModel } from 'src/app/core/store/models/group.model';

@Component({
    selector: 'services-role',
    templateUrl: './services-role.component.html'
})
export class ServicesRoleComponent {
    @Input() role: RoleModel;
    @Input() disabled: boolean;

    @Output() openLightbox: EventEmitter<{}> = new EventEmitter();
    @Output() onRemove: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();
}
