import { Component, ChangeDetectorRef, Input, Output,
    ContentChild, TemplateRef, EventEmitter, OnInit} from "@angular/core";

import { ActivatedRoute, Router } from '@angular/router';
import { RoleModel, GroupModel } from '../../core/store/models';

import { ServicesStore } from '../../services/services.store';

@Component({
    selector: 'services-role-attribution',
    template: `
        <light-box [show]="show" (onClose)="onClose.emit()"> 
            <h1 class="panel-header">{{ 'add.groups' | translate }}</h1>
            <div class="panel-header-sub">
                <button (click)="filterByType('all')">{{ 'all' | translate }}</button>
                <button (click)="filterByType('profile')">{{ 'applications.groups.structure' | translate }}</button>
                <button (click)="filterByType('class')">{{ 'applications.classes' | translate }}</button>
                <button (click)="filterByType('functional')">{{ 'applications.groups.functional' | translate }}</button>
                <button (click)="filterByType('manual')">{{ 'applications.groups.manual' | translate }}</button>
            </div>
            <form>
                <list-component
                [model]="groupList"
                sort="{{ sort }}"
                [inputFilter]="filterByName"
                searchPlaceholder="{{ searchPlaceholder }}"
                noResultsLabel="{{ noResultsLabel }}"
                (inputChange)="groupInputFilter = $event">
                    <ng-template let-item>
                        <span>
                            <span>
                            <input type="checkbox" id="{{ item.id }}" value="{{ item.name }}" name="{{ item.name }}" 
                                [checked]="isAuthorized(item.id, selectedRole)" [(ngModel)]="checkedGroups[item.id]"/>
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
export class ServicesRoleAttributionComponent implements OnInit {
    
    @Input() show;
    @Input() groupList:(GroupModel | {id:string, name:string})[];
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
        this.filterByType('all');
    }


    @Output("onClose") onClose: EventEmitter<any> = new EventEmitter();
    @Output("onAdd") onAdd: EventEmitter<any> = new EventEmitter();
    @Output("inputChange") inputChange: EventEmitter<any> = new EventEmitter<string>();

    @ContentChild(TemplateRef) filterTabsRef:TemplateRef<any>;

    filterByName = (group: any) => {
        if(!this.groupInputFilter) return true;
        return group.name.toLowerCase()
            .indexOf(this.groupInputFilter.toLowerCase()) >= 0;
    }

    filterByType(type: string) {
        if (type == 'all'){
            this.groupList = this.servicesStore.structure.groups.data;
            this.groupList = this.groupList.concat(this.servicesStore.structure.classes);
        }
        else if (type == 'class')
            this.groupList = this.servicesStore.structure.classes;
        else if (type == 'profile')
            this.groupList = this.servicesStore.structure.groups.data.filter(g => g.type == 'ProfileGroup' && g.subType == 'StructureGroup');
        else if (type == 'functional')
            this.groupList = this.servicesStore.structure.groups.data.filter(g => g.type == 'FunctionalGroup');
        else if (type == 'manual')
            this.groupList = this.servicesStore.structure.groups.data.filter(g => g.type == 'ManualGroup');
        this.cdRef.markForCheck();
    }

    isAuthorized(groupId: string, selectedRole: RoleModel) {
        if (selectedRole && selectedRole.groups.findIndex(g => {return g.id == groupId; }) > -1)
            return true;
        else
            return false;
    }

    checkedGroups = {};
    getCheckedGroups() { 
        let arr = [];

        for (let groupId in this.checkedGroups) {
            if (typeof this.checkedGroups[groupId] == 'boolean' && this.checkedGroups[groupId] == true) {
                arr.push(this.groupList.find(g => {return g.id == groupId}));
            }
        }
        this.checkedGroups = {};
        return arr; 
    }
}