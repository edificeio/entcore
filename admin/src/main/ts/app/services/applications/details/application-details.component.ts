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
        <div *ngIf="['1D','2D'].includes(appsTarget[app.icon])" class="message is-warning">
            <div class="message-body">
            {{ 'services.application.message.targetWarning' | translate:{target:appsTarget[app.icon]} }}
            </div>
        </div>
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
    app: any;
    selectedRole: RoleModel;
    showLightbox: boolean = false;
    
    // Dirty hack to display an alert message to ADML about the target (1D or 2D) of the applcation
    appsTarget = {
        'admin-large' : '1D-2D',
        'workspace-large':'1D-2D',
        'conversation-large':'2D-1D',
        'wiki-large':'1D-2D',
        'collaborative-wall-large':'2D',
        'rack-large':'1D-2D',
        'timelinegenerator-large':'1D-2D',
        'bookmark-large':'2D',
        'Xiti-large':'1D-2D',
        'stats-large':'1D',
        'rbs-large':'2D',
        'schoolbook':'1D',
        'scrap-book-large':'1D',
        'searchengine-large':'1D',
        'poll-large':'2D',
        'actualites-large':'2D',
        'mindmap-large':'1D-2D',
        'pages-large':'2D',
        'support-large':'1D-2D',
        'rss-large':'2D',
        'Cursus-large':'2D',
        'statistics-large':'2D',
        'exercizer-large':'2D',
        'community-large':'2D',
        'Maxicours-large':'2D',
        'calendar-large':'2D',
        'pad-large':'2D',
        'cns-large':'2D',
        'forum-large':'2D',
        'sharebigfiles-large':'2D',
        'cahier-de-texte-large':'1D',
        'userbook-large': '1D-2D' // annuaire
    }

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