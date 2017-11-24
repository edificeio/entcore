import { Component, ChangeDetectorRef, Input, Output,
    ContentChild, TemplateRef, EventEmitter, OnInit} from "@angular/core";

import { ActivatedRoute, Router } from '@angular/router';
import { RoleModel, GroupModel } from '../../core/store/models';

import { ServicesStore } from '../../services/services.store';

@Component({
    selector: 'services-role-attribution',
    template: `
        <light-box [show]="show" (onClose)="onClose.emit()">
            <h1 class="panel-header">{{ 'services.roles.groups.add' | translate }}</h1>
            <div class="panel-header-su b">
                <button (click)="filterByType('StructureGroup','ProfileGroup')" [class.active]="visibleGroupType.includes('ProfileGroup')">
                    {{ 'profile.groups' | translate }}</button>
                <button (click)="filterByType('FunctionalGroup')" [class.active]="visibleGroupType.includes('FunctionalGroup')">
                    {{ 'functional.groups' | translate }}</button>
                <button (click)="filterByType('ManualGroup')" [class.active]="visibleGroupType.includes('ManualGroup')">
                    {{ 'manual.groups' | translate }}</button>
            </div>
            <form>
                <list-component
                [model]="groupList"
                sort="{{ sort }}"
                [inputFilter]="filterByName"
                searchPlaceholder="{{ searchPlaceholder }}"
                noResultsLabel="{{ noResultsLabel }}"
                (inputChange)="groupInputFilter = $event"
                (onSelect)="onAdd.emit($event)">
                    <ng-template let-item>
                        <div>{{ item.name }}</div>
                    </ng-template>
                </list-component>
            </form>
        </light-box>
    `
})
export class ServicesRoleAttributionComponent implements OnInit {
    
    @Input() show;
    @Input() groupList:GroupModel[];
    @Input() sort;
    @Input() searchPlaceholder;
    @Input() noResultsLabel;
    @Input() selectedRole:RoleModel;
    groupInputFilter:string;

    constructor(
        private cdRef: ChangeDetectorRef,
        private servicesStore:ServicesStore
    ){}

    ngOnInit() {
        this.filterByType();
    }

    @Output("onClose") onClose: EventEmitter<any> = new EventEmitter();
    @Output("onAdd") onAdd: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();
    @Output("inputChange") inputChange: EventEmitter<any> = new EventEmitter<string>();

    @ContentChild(TemplateRef) filterTabsRef:TemplateRef<any>;

    filterByName = (group: any) => {
        if(!this.groupInputFilter) return true;
        return group.name.toLowerCase()
            .indexOf(this.groupInputFilter.toLowerCase()) >= 0;
    }

    visibleGroupType:string[] = [];

    filterByType(...types:string[]) {
        this.groupList = this.servicesStore.structure.groups.data;
        // Do not display groups if tey are already linked to the selected role
        if (this.selectedRole) {
            let selectedGroupId:string[] = this.selectedRole.groups.map(g => g.id);
            this.groupList = this.groupList.filter(
                g => !selectedGroupId.includes(g.id)
            );    
        }
        
        if (types != undefined && types.length > 0) {
            types.forEach(type => {
                if (this.visibleGroupType.includes(type))
                    this.visibleGroupType.splice(this.visibleGroupType.indexOf(type),1);
                else
                    this.visibleGroupType.push(type)
            });
            this.groupList = this.groupList.filter(g => this.visibleGroupType.includes(g.type));
        }
        this.cdRef.markForCheck();
    }

}