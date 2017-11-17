import { Component, OnInit, OnDestroy, ChangeDetectionStrategy,
    ChangeDetectorRef, Input, ViewChild } from "@angular/core";
import { ActivatedRoute, Router,  Data } from '@angular/router';

import { Subscription } from 'rxjs/Subscription';
import { SpinnerService, routing } from '../../../core/services';
import { globalStore } from '../../../core/store';

import { ServicesStore } from '../../services.store';
import { ConnectorModel, RoleModel, GroupModel } from '../../../core/store/models';
import { ServicesRoleAttributionComponent } from '../../shared/services-role-attribution.component'

@Component({
    selector: 'connector-details',
    template: `
        <div class="panel-header">
            {{ 'services.rights.give' | translate }}
        </div>

        <div class="panel-section">
            <div *ngIf="connector.roles.length == 0" class="message is-warning">
                <div class="message-body">
                    {{ 'services.connector.roles.list.empty' | translate }}
                </div>
            </div>
            
            <div *ngFor="let role of connector.roles" class="has-vertical-padding">
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
            (onAdd)="addGroupsToRole()"
            [selectedRole]="selectedRole"
        >
        </services-role-attribution>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConnectorDetailsComponent  implements OnInit, OnDestroy {
    
    @ViewChild(ServicesRoleAttributionComponent) roleAttributionComponent; 
    connector: ConnectorModel;
    selectedRole: RoleModel;
    showLightbox: boolean = false;
    
    private connectorSubscriber: Subscription;
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
            if (params['connectorId']) {
                this.servicesStore.connector = this.servicesStore.structure
                    .connectors.data.find(a => a.id === params['connectorId']);
                this.connector = this.servicesStore.connector;
                this.cdRef.markForCheck();
            }
        })
        
        this.connectorSubscriber = this.route.data.subscribe(data => {
            if(data["roles"]) {
                this.servicesStore.connector.roles = data["roles"];
                this.connector.roles = this.servicesStore.connector.roles;
                this.cdRef.markForCheck();
            }
        })
    }

    ngOnDestroy(): void {
        this.routeSubscriber.unsubscribe();
        this.connectorSubscriber.unsubscribe();
    }

    openLightbox(role: RoleModel){
        this.selectedRole = role;
        this.showLightbox = true;
    }

    addGroupsToRole(): void {
        this.servicesStore.connector.roles.find(r => r.id == this.selectedRole.id)
            .addGroupsToRole(this.roleAttributionComponent  .getCheckedGroups());
        this.showLightbox = false;
        this.cdRef.markForCheck();
    }

    removeGroupFromRole(group:GroupModel, roleId: string): void {
        let role = this.servicesStore.connector.roles.find(role => role.id == roleId);
        role.removeGroupFromRole(group)
            .then(() => {
                this.cdRef.markForCheck();
            })
    }
}