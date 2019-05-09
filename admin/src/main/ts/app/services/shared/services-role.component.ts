import { Component, Input, Output, EventEmitter } from "@angular/core";
import { RoleModel, GroupModel } from '../../core/store/models';

@Component({
    selector: 'services-role',
    template: `
        <panel-section section-title="{{ role.name }}" [folded]="false">
            <button (click)="openLightbox.emit(role)" [disabled]="disabled">
                {{ 'add.groups' | translate }}
                <i class="fonticon group_add is-size-3"></i>
            </button>
            
            <div *ngIf="role.groups.length == 0" class="message is-warning">
                <div class="message-body">
                    <s5l>services.roles.groups.empty</s5l>
                </div>
            </div>

            <div>
                <h4 *ngIf="role.subStructures != null && role.subStructures.length > 0">
                    {{ 'groups.local' | translate }}
                </h4>
                <ul class="actions-list">
                    <li *ngFor="let group of role.groups">
                        <span>{{ group.name }}</span>
                        <i class="fa fa-times action" (click)="onRemove.emit(group)"></i>
                    </li>
                </ul>
            </div>

            <div *ngIf="role.subStructures != null && role.subStructures.length > 0">
                <h4>{{ 'role.substructure' | translate }}</h4>
                <ul class="actions-list">
                    <li *ngFor="let s of role.subStructures">
                        <span>{{ s }}</span>
                    </li>
                </ul>
            </div>
        </panel-section>
    `
})
export class ServicesRoleComponent {
    @Input() role: RoleModel;
    @Input() disabled: boolean;

    @Output("openLightbox") openLightbox: EventEmitter<{}> = new EventEmitter();
    @Output("onRemove") onRemove: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();
}