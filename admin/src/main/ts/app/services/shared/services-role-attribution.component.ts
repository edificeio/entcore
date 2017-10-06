import { Component, ChangeDetectorRef, Input, Output,
    ContentChild, TemplateRef, EventEmitter, AfterViewInit } from "@angular/core";

import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'services-role-attribution',
    template: `
        <light-box [show]="show" (onClose)="onClose.emit()"> 
            <h1 class="panel-header">{{ 'add.groups' | translate }}</h1>
            <ng-template [ngTemplateOutlet]="filterTabsRef">
            </ng-template>
            <form>
                <list-component
                [model]="model"
                sort="{{ sort }}"
                [inputFilter]="inputFilter"
                searchPlaceholder="{{ searchPlaceholder }}"
                noResultsLabel="{{ noResultsLabel }}"
                (inputChange)="inputChange.emit($event)">
                    <ng-template let-item>
                        <span>
                            <span [ngSwitch]="isAuthorized(item.id, selectedRole)">
                                <input *ngSwitchCase="true" type="checkbox" id="{{ item.id }}" value="{{ item.name }}" checked />
                                <input *ngSwitchDefault type="checkbox" id="{{ item.id }}" value="{{ item.name }}" />
                            </span>
                            <label>{{ item.name }}</label>                        
                        </span>
                    </ng-template>
                </list-component>
                <button type="submit" (click)="onAdd.emit()">{{ 'save' | translate }}</button>
            </form>
        </light-box>
    `
})
export class ServicesRoleAttributionComponent {
    
    @Input() show;
    @Input() model;
    @Input() sort;
    @Input() inputFilter;
    @Input() searchPlaceholder;
    @Input() noResultsLabel;
    @Input() isAuthorized
    @Input() selectedRole

    @Output("onClose") onClose: EventEmitter<any> = new EventEmitter();
    @Output("onAdd") onAdd: EventEmitter<any> = new EventEmitter();
    @Output("inputChange") inputChange: EventEmitter<any> = new EventEmitter<string>();

    @ContentChild(TemplateRef) filterTabsRef:TemplateRef<any>;
}