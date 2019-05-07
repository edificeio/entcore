import { Component, Input, Output, EventEmitter } from "@angular/core";
import { ConnectorModel, RoleModel, GroupModel } from '../../../../core/store/models';

@Component({
    selector: 'connector-assignment',
    template: `
        <div class="panel-section">
            <div *ngIf="connector.roles.length == 0" class="message is-warning has-margin-10">
                <div class="message-body">
                    {{ 'services.connector.roles.list.empty' | translate }}
                </div>
            </div>
            
            <div *ngFor="let role of connector.roles">
                <services-role
                    [role]="role"
                    (openLightbox)="openRoleAttributionLightbox($event)"
                    (onRemove)="remove.emit({group: $event, role: role})">
                </services-role>
            </div>
        </div>

        <services-role-attribution
            [show]="showRoleAttributionLightbox"
            (close)="showRoleAttributionLightbox = false"
            sort="name"
            searchPlaceholder="search.group"
            noResultsLabel="list.results.no.groups"
            (add)="add.emit({group: $event, role: selectedRole})"
            [selectedRole]="selectedRole">
        </services-role-attribution>
    `
})
export class ConnectorAssignmentComponent {
    @Input()
    connector: ConnectorModel;

    @Output()
    remove: EventEmitter<{group: GroupModel, role: RoleModel}> = new EventEmitter();
    @Output()
    add: EventEmitter<{group: GroupModel, role: RoleModel}> = new EventEmitter(); 

    selectedRole: RoleModel;
    showRoleAttributionLightbox: boolean = false;

    public openRoleAttributionLightbox(role: RoleModel){
        this.selectedRole = role;
        this.showRoleAttributionLightbox = true;
    }
}