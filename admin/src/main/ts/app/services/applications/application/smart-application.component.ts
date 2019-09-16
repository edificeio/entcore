import { Component, OnInit, OnDestroy } from "@angular/core";
import { Subscription } from "rxjs";
import { ActivatedRoute, Data } from "@angular/router";
import { ServicesStore } from "../../services.store";
import { RoleModel, GroupModel, StructureModel } from "../../../core/store";
import { ServicesService } from "../../services.service";
import { routing } from "../../../core/services";

@Component({
    selector: 'smart-application',
    template: `
        <div class="panel-header">
            <span>{{ servicesStore.application.displayName | translate }}</span>
        </div>

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

        <application-assignment 
            *ngIf="currentTab  === 'assignment'"
            [application]="servicesStore.application"
            [assignmentGroupPickerList]="assignmentGroupPickerList"
            (remove)="onRemoveAssignment($event)"
            (add)="onAddAssignment($event)">
        </application-assignment>

        <smart-mass-role-assignment *ngIf="currentTab  === 'massAssignment'"
                                    (massAssignment)="onMassAssignment()">
        </smart-mass-role-assignment>
    `,
    styles: [`
        button.tab {
            border-left: 0;
            box-shadow: none;
            border-right: 0;
            border-top: 0;
            margin: 0 10px;
            padding-left: 10px;
            padding-right: 10px;
        }
    `, `
        button.tab:not(active):hover {
            color: #ff8352;
            background-color: #fff;
            border-bottom-color: #ff8352;
        }
    `]
})
export class SmartApplicationComponent implements OnInit, OnDestroy {
    public currentTab: 'assignment' | 'massAssignment' = 'assignment';

    private routeParamsSubscription: Subscription;
    private rolesSubscription: Subscription;
    private structureSubscriber: Subscription;
    public assignmentGroupPickerList: GroupModel[];

    constructor(private activatedRoute: ActivatedRoute,
        public servicesStore: ServicesStore,
        private servicesService: ServicesService) {
    }

    ngOnInit(): void {
        this.routeParamsSubscription = this.activatedRoute.params.subscribe(params => {
            if (params['appId']) {
                this.servicesStore.application = this.servicesStore.structure
                    .applications.data.find(a => a.id === params['appId']);
            }
        });

        this.rolesSubscription = this.activatedRoute.data.subscribe(data => {
            if (data['roles']) {
                this.servicesStore.application.roles = data['roles'];
                this.servicesStore.application.roles = filterRolesByDistributions(
                    this.servicesStore.application.roles.filter(r => r.transverse == false),
                    this.servicesStore.structure.distributions);
            }
        });

        this.structureSubscriber = routing.observe(this.activatedRoute, 'data').subscribe((data: Data) => {
            if (data['structure']) {
                this.assignmentGroupPickerList = this.servicesStore.structure.groups.data;
                if (!this.structureHasChildren(this.servicesStore.structure) && this.currentTab === 'massAssignment') {
                    this.currentTab = 'assignment';
                }
            }
        });
    }

    ngOnDestroy(): void {
        this.routeParamsSubscription.unsubscribe();
        this.rolesSubscription.unsubscribe();
        this.structureSubscriber.unsubscribe();
    }

    public onAddAssignment($event: {group: GroupModel, role: RoleModel}) {
        $event.role.addGroup($event.group);
    }

    public onRemoveAssignment($event: {group: GroupModel, role: RoleModel}): void {
        $event.role.removeGroup($event.group);
    }

    public onMassAssignment(): void {
        this.servicesStore.application.syncRoles(this.servicesStore.structure.id)
            .then(() => 
                this.servicesStore.application.roles = filterRolesByDistributions(
                    this.servicesStore.application.roles.filter(r => r.transverse == false),
                    this.servicesStore.structure.distributions)
            );
    }

    public structureHasChildren(structure: StructureModel): boolean {
        return structure.children && structure.children.length > 0;
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