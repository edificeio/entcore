import { Component, ChangeDetectorRef, Input, Output,
    ContentChild, TemplateRef, EventEmitter, AfterViewInit } from "@angular/core";

import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'services-role',
    template: `
        <panel-section section-title="{{ role.name }}" *ngIf="role.transverse == false">
            <button (click)="openLightbox.emit(role)">
                {{ 'add.groups' | translate }}
                <i class="fa fa-plus"></i>
            </button>
            <div class="flex-container">
                <div *ngFor="let group of (role.groups | mapToArray)" class="flex-item">
                    <label>{{ group.value }}</label>
                    <i class="fa fa-times action" (click)="onRemove.emit(group.key)"></i>
                </div>
            </div>
        </panel-section>
    `
})
export class ServicesRoleComponent {
    
    @Input() role

    @Output("openLightbox") openLightbox: EventEmitter<{}> = new EventEmitter();
    @Output("onRemove") onRemove: EventEmitter<string> = new EventEmitter();
}