import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,
    ChangeDetectorRef, Input } from "@angular/core";
import { ActivatedRoute, Router,  Data } from '@angular/router';

import { Subscription } from 'rxjs/Subscription';
import { SpinnerService, routing } from '../../../core/services';
import { globalStore } from '../../../core/store';

import { ServicesStore } from '../../services.store';
import { ApplicationModel, RoleModel } from '../../../core/store/models';

@Component({
    selector: 'app-details',
    template: `
        <div class="panel-header" [ngSwitch]="app.roles.length">
            <span *ngSwitchCase="0">{{ 'list.no.role' | translate }}</span>
            <span *ngSwitchDefault>{{ 'application.give.rights' | translate }} {{ app.name }}</span>
        </div>
        <div *ngFor="let role of app.roles">
            <services-role
                [role]="role"
                (openLightbox)="openLightbox($event)"
                (onRemove)="removeGroupFromRole($event, role.id)"
            >
            </services-role>
        </div>
        <services-role-attribution
            [show]="showLightbox"
            (onClose)="showLightbox = false"
            [model]="this.groupsList"
            sort="name"
            [inputFilter]="filterByName"
            searchPlaceholder="search.group"
            noResultsLabel="list.results.no.groups"
            (inputChange)="this.groupInputFilter = $event"
            (onAdd)="addGroupsToRole()"
            [isAuthorized]="isAuthorized"
            [selectedRole]="selectedRole"
        >
            <ng-template>
                <div class="panel-header-sub">
                    <button (click)="filterByType('all')">{{ 'all' | translate }}</button>
                    <button (click)="filterByType('profile')">{{ 'applications.groups.structure' | translate }}</button>
                    <button (click)="filterByType('class')">{{ 'applications.classes' | translate }}</button>
                    <button (click)="filterByType('functional')">{{ 'applications.groups.functional' | translate }}</button>
                    <button (click)="filterByType('manual')">{{ 'applications.groups.manual' | translate }}</button>
                </div>
            </ng-template>
        </services-role-attribution>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConnectorDetailsComponent  implements OnInit, OnDestroy {
    
    app: ApplicationModel;
    selectedRole: RoleModel;
    showLightbox: boolean = false;
    groupInputFilter: string;
    groupsList: {}[];
    
    private appSubscriber: Subscription;
    private routeSubscriber: Subscription;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private cdRef: ChangeDetectorRef,
        private ls: SpinnerService,
        public servicesStore: ServicesStore
    ) {}

    ngOnInit(): void {
        this.routeSubscriber = this.route.params.subscribe(params => {
            if (params['appId']) {
                this.servicesStore.application = this.servicesStore.structure
                    .applications.data.find(a => a.id === params['appId']);
                this.app = this.servicesStore.application;
                this.cdRef.markForCheck();
            }
        })
        
        this.appSubscriber = this.route.data.subscribe(data => {
            if(data["roles"]) {
                this.servicesStore.application.roles = data["roles"];
                this.app.roles = this.servicesStore.application.roles;
                this.cdRef.markForCheck();
            }
        })

        this.filterByType('all');
    }

    ngOnDestroy(): void {
        this.routeSubscriber.unsubscribe();
        this.appSubscriber.unsubscribe();
    }

    filterByName = (group: any) => {
        if(!this.groupInputFilter) return true;
        return group.name.toLowerCase()
            .indexOf(this.groupInputFilter.toLowerCase()) >= 0;
    }

    filterByType(type: string) {
        if (type == 'all'){
            this.groupsList = this.servicesStore.structure.groups.data;
            this.groupsList = this.groupsList.concat(this.servicesStore.structure.classes);
        }
        else if (type == 'class')
            this.groupsList = this.servicesStore.structure.classes;
        else if (type == 'profile')
            this.groupsList = this.servicesStore.structure.groups.data.filter(g => g.type == 'ProfileGroup' && g.subType == 'StructureGroup');
        else if (type == 'functional')
            this.groupsList = this.servicesStore.structure.groups.data.filter(g => g.type == 'FunctionalGroup');
        else if (type == 'manual')
            this.groupsList = this.servicesStore.structure.groups.data.filter(g => g.type == 'ManualGroup');
        this.cdRef.markForCheck();
    }

    openLightbox(role: RoleModel){
        this.selectedRole = role;
        this.showLightbox = true;
    }

    isAuthorized(groupId: string, selectedRole: RoleModel) {
        if (selectedRole && selectedRole.groups.has(groupId))
            return true;
        else
            return false;
    }

    addGroupsToRole(): void {
        let groups = this.getCheckedGroups();

        this.servicesStore.application.roles.find(r => r.id == this.selectedRole.id)
            .addGroupsToRole(groups);
        this.showLightbox = false;

        this.cdRef.markForCheck();
    }

    private getCheckedGroups() {

        let arr = [];
        let elmts = document.querySelectorAll('input[type=checkbox]:checked');

        for (let i = 0; i < elmts.length; i++)
            arr.push({id: elmts[i].id, name: elmts[i].attributes.getNamedItem('value').value});

        return arr;
    }

    removeGroupFromRole(groupId: string, roleId: string): void {
        let role = this.servicesStore.application.roles.find(role => role.id == roleId);
        role.removeGroupFromRole(groupId)
            .then(() => {
                this.cdRef.markForCheck();
            })
    }
}