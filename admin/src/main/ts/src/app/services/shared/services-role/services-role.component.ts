import {Component, EventEmitter, Input, Output} from '@angular/core';
import {GroupModel, RoleModel} from '../../../core/store/models';

@Component({
    selector: 'services-role',
    templateUrl: './services-role.component.html'
})
export class ServicesRoleComponent {
    @Input() role: RoleModel;
    @Input() disabled: boolean;

    @Output('openLightbox') openLightbox: EventEmitter<{}> = new EventEmitter();
    @Output('onRemove') onRemove: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();
}
