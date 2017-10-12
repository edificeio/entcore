import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,
    ChangeDetectorRef, Input, ViewChild } from "@angular/core";
import { ActivatedRoute, Router,  Data } from '@angular/router';

import { Subscription } from 'rxjs/Subscription';
import { SpinnerService, routing } from '../../../core/services';
import { globalStore } from '../../../core/store';

import { ServicesStore } from '../../services.store';

import { ApplicationModel, RoleModel, GroupModel } from '../../../core/store/models';
import { ServicesRoleAttributionComponent } from '../../shared/services-role-attribution.component';

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
            sort="name"
            searchPlaceholder="search.group"
            noResultsLabel="list.results.no.groups"
            (onAdd)="addGroupsToRole()"
            [selectedRole]="selectedRole"
        >
        </services-role-attribution>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApplicationDetailsComponent  implements OnInit, OnDestroy {
    
    @ViewChild(ServicesRoleAttributionComponent) roleAttributionComponent; 
    app: ApplicationModel;
    selectedRole: RoleModel;
    showLightbox: boolean = false;
    
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
    }

    ngOnDestroy(): void {
        this.routeSubscriber.unsubscribe();
        this.appSubscriber.unsubscribe();
    }

    openLightbox(role: RoleModel){
        this.selectedRole = role;
        this.showLightbox = true;
    }

    addGroupsToRole(): void {
        this.servicesStore.application.roles.find(r => r.id == this.selectedRole.id)
            .addGroupsToRole(this.roleAttributionComponent.getCheckedGroups());
        this.showLightbox = false;
        this.cdRef.markForCheck();
    }

    removeGroupFromRole(group: GroupModel, roleId: string): void {
        let role = this.servicesStore.application.roles.find(role => role.id == roleId);
        role.removeGroupFromRole(group)
            .then(() => {
                this.cdRef.markForCheck();
            })
    }
}