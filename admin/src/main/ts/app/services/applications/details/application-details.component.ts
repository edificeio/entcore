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
        <div class="panel-header">
            {{ 'services.rights.give' | translate }}
        </div>

        <div class="panel-section">
            <div *ngIf="app.roles.length == 0" class="message is-warning">
                <div class="message-body">
                    {{ 'services.application.roles.list.empty' | translate }}
                </div>
            </div>
            
            <div *ngFor="let role of app.roles" class="has-vertical-padding">
                <services-role
                    [role]="role"
                    (openLightbox)="openLightbox($event)"
                    (onRemove)="removeGroupFromRole($event, role.id)">
                </services-role>
            </div>
        </div>
        
        
        <services-role-attribution
            [show]="showLightbox"
            (onClose)="showLightbox = false"
            sort="name"
            searchPlaceholder="search.group"
            noResultsLabel="list.results.no.groups"
            (onAdd)="addGroupToSelectedRole($event)"
            [selectedRole]="selectedRole">
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
                this.app.roles = this.servicesStore.application.roles.filter(r => r.transverse == false);
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

    async addGroupToSelectedRole(group:GroupModel) {
        let res = await this.selectedRole.addGroup(group);
        this.cdRef.markForCheck();
    }

    removeGroupFromRole(group: GroupModel, roleId: string): void {
        let role = this.servicesStore.application.roles.find(role => role.id == roleId);
        role.removeGroup(group)
            .then(() => {
                this.cdRef.markForCheck();
            })
    }
}