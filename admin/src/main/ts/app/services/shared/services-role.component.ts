import { Component, ChangeDetectorRef, Input, Output,
    ContentChild, TemplateRef, EventEmitter, AfterViewInit } from "@angular/core";

import { ActivatedRoute, Router } from '@angular/router';

import { RoleModel, GroupModel } from '../../core/store/models';

@Component({
    selector: 'services-role',
    template: `
        <panel-section section-title="{{ role.name }}" [folded]="false">
            <button class="add-groups" (click)="openLightbox.emit(role)">
                {{ 'add.groups' | translate }}
                <i class="fonticon group_add"></i>
            </button>
            <div *ngIf="role.groups.length == 0" class="message is-warning">
                <div class="message-body">
                    <s5l>services.roles.groups.empty</s5l>
                </div>
            </div>
            <div class="flex-container" *ngIf="role.groups.length > 0">
                <div>
                    <h4 *ngIf="role.subStructures != null && role.subStructures.length > 0">{{ 'groups.local' | translate }}</h4>
                    <div *ngFor="let group of role.groups" class="flex-item">
                        <label>{{ group.name }}</label>
                        <i class="fa fa-times action" (click)="onRemove.emit(group)"></i>
                    </div>    
                </div>
                <div *ngIf="role.subStructures != null && role.subStructures.length > 0">
                    <h4>{{ 'role.substructure' | translate }}</h4>
                    <div *ngFor="let s of role.subStructures" class="flex-item">
                        <label>{{ s }}</label>
                    </div>   
                </div>
            </div>
        </panel-section>
    `
})
export class ServicesRoleComponent {
    
    @Input() role: RoleModel;

    @Output("openLightbox") openLightbox: EventEmitter<{}> = new EventEmitter();
    @Output("onRemove") onRemove: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();

    constructor(public cdRef: ChangeDetectorRef){}

}