import { Component, ChangeDetectorRef, Input, Output,
    ContentChild, TemplateRef, EventEmitter, AfterViewInit } from "@angular/core";

import { ActivatedRoute, Router } from '@angular/router';

import { RoleModel, GroupModel } from '../../core/store/models';

@Component({
    selector: 'services-role',
    template: `
        <panel-section section-title="{{ role.name }}" [folded]="false">
            <!-- Button removed for Beta 1 -->
            <!--<button (click)="openLightbox.emit(role)">
                {{ 'add.groups' | translate }}
                <i class="fa fa-plus"></i>
            </button>-->

            <div *ngIf="role.groups.length == 0" class="message is-warning">
                <div class="message-body">
                    <s5l>list.role.no.groups</s5l>
                </div>
            </div>
            <div class="flex-container" *ngIf="role.groups.length > 0">
                <div *ngFor="let group of role.groups" class="flex-item">
                    <label>{{ group.name }}</label>

                    <!-- Button removed for Beta 1 -->
                    <!--<i class="fa fa-times action" (click)="onRemove.emit(group)"></i>-->
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