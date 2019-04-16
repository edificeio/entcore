import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { ServicesStore } from '../../services.store';
import { GroupModel, RoleModel, StructureModel } from '../../../core/store/models';

@Component({
    selector: 'app-details',
    template: `
        <div class="tabs" *ngIf="structureHasChildren(servicesStore.structure)">
            <button class="tab"
                    [ngClass]="{active: currentTab === 'assignment'}"
                    (click)="currentTab = 'assignment'">
                {{ 'services.tab.assignment' | translate }}
            </button>
            <button class="tab"
                    [ngClass]="{active: currentTab === 'massAssignment'}"
                    (click)="currentTab = 'massAssignment'">
                {{ 'services.tab.mass-assignment' | translate }}
            </button>
        </div>
        <smart-mass-role-assignment *ngIf="currentTab  === 'massAssignment'"
                                    (massAssignment)="onMassAssignment()"></smart-mass-role-assignment>
        <div *ngIf="currentTab  === 'assignment'">
            <div *ngIf="['1D','2D'].includes(appsTarget[app.icon])" class="message is-warning has-margin-10">
                <div class="message-body">
                    {{ 'services.application.message.targetWarning' | translate:{target: appsTarget[app.icon]} }}
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
                            (openLightbox)="openRoleAttribution($event)"
                            (onRemove)="removeGroupFromRole($event, role.id)">
                    </services-role>
                </div>
            </div>
            <services-role-attribution
                    [show]="showRoleAttribution"
                    (close)="showRoleAttribution = false"
                    sort="name"
                    searchPlaceholder="search.group"
                    noResultsLabel="list.results.no.groups"
                    (add)="addGroupToSelectedRole($event)"
                    [selectedRole]="selectedRole">
            </services-role-attribution>
        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    styles: [`
        .tabs {
            font-size: 0;
            margin-bottom: 0;
        }`, `
        .tabs > .tab {
            box-shadow: none;
            border-left: 0;
            border-right: 0;
            border-top: 0;
            border-radius: 0;
            font-size: 16px;
        }`
    ]
})
export class ApplicationDetailsComponent implements OnInit, OnDestroy {
    public app: any;
    public selectedRole: RoleModel;
    public showRoleAttribution = false;
    public currentTab: 'assignment' | 'massAssignment' = 'assignment';

    // Dirty hack to display an alert message to ADML about the target (1D or 2D) of the application
    public appsTarget = appsTarget;

    private routeDataSubscription: Subscription;
    private routeParamsSubscription: Subscription;

    constructor(
        public servicesStore: ServicesStore,
        private route: ActivatedRoute,
        private router: Router,
        private changeDetectorRef: ChangeDetectorRef
    ) {
    }

    ngOnInit(): void {
        this.routeParamsSubscription = this.route.params.subscribe(params => {
            if (params['appId']) {
                this.servicesStore.application = this.servicesStore.structure
                    .applications.data.find(a => a.id === params['appId']);
                this.app = this.servicesStore.application;
                this.currentTab = 'assignment';
                this.changeDetectorRef.markForCheck();
            }
        });

        this.routeDataSubscription = this.route.data.subscribe(data => {
            if (data['roles']) {
                this.servicesStore.application.roles = data['roles'];
                this.app.roles = filterRolesByDistributions(
                    this.servicesStore.application.roles.filter(r => r.transverse == false),
                    this.servicesStore.structure.distributions);
                this.currentTab = 'assignment';
                this.changeDetectorRef.markForCheck();
            }
        });

        this.currentTab = 'assignment';
    }

    onMassAssignment() {
        this.servicesStore.application.syncRoles(this.servicesStore.structure.id)
            .then(() => this.changeDetectorRef.markForCheck());
    }

    openRoleAttribution(role: RoleModel) {
        this.selectedRole = role;
        this.showRoleAttribution = true;
    }

    addGroupToSelectedRole(group: GroupModel) {
        this.selectedRole
            .addGroup(group)
            .then(() => this.changeDetectorRef.markForCheck());
    }

    removeGroupFromRole(group: GroupModel, roleId: string): void {
        this.servicesStore.application.roles
            .find(role => role.id == roleId)
            .removeGroup(group)
            .then(() => this.changeDetectorRef.markForCheck());
    }

    structureHasChildren(structure: StructureModel) {
        return structure.children && structure.children.length > 0;
    }

    ngOnDestroy(): void {
        this.routeParamsSubscription.unsubscribe();
        this.routeDataSubscription.unsubscribe();
    }
}

export function filterRolesByDistributions(roles: RoleModel[], distributions: string[]): RoleModel[] {
    return roles.filter(role => {
        if (role.distributions.length === 0) {
            return true;
        }
        return distributions.some(distribution => role.distributions.indexOf(distribution) >= 0);
    });
}

const appsTarget = {
    'admin-large': '1D-2D',
    'workspace-large': '1D-2D',
    'conversation-large': '2D-1D',
    'wiki-large': '1D-2D',
    'collaborative-wall-large': '2D',
    'rack-large': '1D-2D',
    'timelinegenerator-large': '1D-2D',
    'bookmark-large': '2D',
    'Xiti-large': '1D-2D',
    'stats-large': '1D',
    'rbs-large': '2D',
    'schoolbook': '1D',
    'scrap-book-large': '1D-2D',
    'searchengine-large': '1D',
    'poll-large': '2D',
    'actualites-large': '1D-2D',
    'mindmap-large': '1D-2D',
    'pages-large': '2D',
    'support-large': '1D-2D',
    'rss-large': '2D',
    'Cursus-large': '2D',
    'statistics-large': '2D',
    'exercizer-large': '2D',
    'community-large': '2D',
    'Maxicours-large': '2D',
    'calendar-large': '2D',
    'pad-large': '1D-2D',
    'cns-large': '2D',
    'forum-large': '2D',
    'sharebigfiles-large': '2D',
    'cahier-de-texte-large': '1D',
    'userbook-large': '1D-2D',
    'settings-class-large': '1D',
    'competences': '2D',
    'parametrage': '2D'
};
