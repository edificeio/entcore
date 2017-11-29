import { Component, ChangeDetectorRef, Input, Output,
    ContentChild, TemplateRef, EventEmitter, OnInit} from "@angular/core";

import { ActivatedRoute, Router } from '@angular/router';
import { RoleModel, GroupModel } from '../../core/store/models';

import { ServicesStore } from '../../services/services.store';

@Component({
    selector: 'services-role-attribution',
    template: `
        <light-box [show]="show" (onClose)="doOnClose()" class="inner-list">
            <div class="padded">
                <h3>{{ 'services.roles.groups.add' | translate }}</h3>
                
                <div class="filters">
                    <button (click)="filterByType('StructureGroup','ProfileGroup')" 
                        [class.selected]="visibleGroupType.includes('ProfileGroup')">
                        {{ 'profile.groups' | translate }} <i class="fa fa-filter is-size-5"></i>
                    </button>
                    
                    <button (click)="filterByType('FunctionalGroup')" 
                        [class.selected]="visibleGroupType.includes('FunctionalGroup')">
                        {{ 'functional.groups' | translate }} <i class="fa fa-filter is-size-5"></i>
                    </button>
                    
                    <button (click)="filterByType('ManualGroup')" 
                        [class.selected]="visibleGroupType.includes('ManualGroup')">
                        {{ 'manual.groups' | translate }} <i class="fa fa-filter is-size-5"></i>
                    </button>
                </div>

                <form>
                    <list-component
                        [model]="groupList"
                        [sort]="sort"
                        [filters]="filterGroups"
                        [inputFilter]="filterByInput"
                        [searchPlaceholder]="searchPlaceholder"
                        [noResultsLabel]="noResultsLabel"
                        (inputChange)="groupInputFilter = $event"
                        (onSelect)="onAdd.emit($event)">
                        <ng-template let-item>
                            <div>{{ item.name }}</div>
                        </ng-template>
                    </list-component>
                </form>
            </div>
        </light-box>`
})
export class ServicesRoleAttributionComponent implements OnInit {
    
    @Input() show;
    @Input() groupList:GroupModel[];
    @Input() sort;
    @Input() searchPlaceholder;
    @Input() noResultsLabel;
    @Input() selectedRole:RoleModel;
    
    @Output("onClose") onClose: EventEmitter<any> = new EventEmitter();
    @Output("onAdd") onAdd: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();
    @Output("inputChange") inputChange: EventEmitter<any> = new EventEmitter<string>();
    
    @ContentChild(TemplateRef) filterTabsRef:TemplateRef<any>;
    
    groupInputFilter:string;
    visibleGroupType:string[] = [];

    constructor(
        private cdRef: ChangeDetectorRef,
        private servicesStore:ServicesStore){}

    ngOnInit() {
        this.groupList = this.servicesStore.structure.groups.data;
    }

    filterByInput = (group: any) => {
        if(!this.groupInputFilter) return true;
        return group.name.toLowerCase().indexOf(this.groupInputFilter.toLowerCase()) >= 0;
    }

    filterGroups = (group: GroupModel) => {
        // Do not display groups if they are already linked to the selected role
        if (this.selectedRole) {
            let selectedGroupId:string[] = this.selectedRole.groups.map(g => g.id);
            return !selectedGroupId.find(g => g == group.id);
        }
        return true;
    }

    filterByType = (...types:string[]) => {        
        if (types != undefined && types.length > 0) {
            types.forEach(type => {
                if (this.visibleGroupType.includes(type)) {
                    this.visibleGroupType.splice(this.visibleGroupType.indexOf(type),1);
                } else {
                    this.visibleGroupType.push(type);
                }
            });

            if (this.visibleGroupType && this.visibleGroupType.length > 0) {
                this.groupList = this.servicesStore.structure.groups.data.filter(g => this.visibleGroupType.includes(g.type));
            } else {
                this.groupList = this.servicesStore.structure.groups.data;
            }
        }
        this.cdRef.markForCheck();
    }

    doOnClose() {
        this.onClose.emit(); 
        // component is not destroy on close, we reset these properties for next opening
        this.visibleGroupType = [];
        this.groupList = this.servicesStore.structure.groups.data;
    }
}