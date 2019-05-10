import { Component, OnInit, OnDestroy } from "@angular/core";
import { Subscription } from "rxjs";
import { CasType } from "./CasType";
import { ConnectorModel, Session, SessionModel, GroupModel, RoleModel } from "../../../core/store";
import { ServicesService } from "../../services.service";
import { ActivatedRoute, Params, Router } from "@angular/router";
import { ServicesStore } from "../../services.store";
import { SpinnerService, NotifyService } from "../../../core/services";
import { Location } from "@angular/common";
import { BundlesService } from "sijil";

import 'rxjs/add/operator/do';
import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/toPromise';
import { NgForm } from "@angular/forms";

@Component({
    selector: 'smart-connector',
    template: `
        <div class="panel-header">
            <span *ngIf="isCreationMode() else isEditionMode">
                <s5l>services.connector.create.title</s5l>
            </span>
            <ng-template #isEditionMode>
                <span>{{ servicesStore.connector.displayName }}</span>
                <i *ngIf="isInherited()"
                    class="fa fa-link has-left-margin-5"
                    [title]="'services.connector.inherited' | translate">
                </i>
                <i *ngIf="isLocked()"
                    class="fa fa-lock has-left-margin-5"
                    [title]="'services.connector.locked' | translate">
                </i>

                <button type="button" 
                        class="is-danger is-pulled-right has-left-margin-5" 
                        (click)="showDeleteConfirmation = true;"
                        [disabled]="arePropertiesDisabled()">
                    <s5l>services.connector.delete.button</s5l>
                    <i class="fa fa-trash is-size-5"></i>
                </button>

                <button
                    type="button" 
                    class="is-pulled-right has-left-margin-5"
                    (click)="save()"
                    *ngIf="currentTab === PROPERTIES_TAB"
                    [disabled]="arePropertiesDisabled()">
                    <s5l>save.modifications</s5l>
                    <i class="fa fa-floppy-o is-size-5"></i>
                </button>

                <button type="button"
                        class="is-pulled-right"
                        *ngIf="admc"
                        (click)="lockToggle();">
                    <span *ngIf="isLocked() else isUnlocked">
                        <s5l>services.connector.unlock.button</s5l>
                        <i class="fa fa-unlock is-size-5"></i>
                    </span>
                    <ng-template #isUnlocked>
                        <span>
                            <s5l>services.connector.lock.button</s5l>
                            <i class="fa fa-lock is-size-5"></i>
                        </span>
                    </ng-template>
                </button>
            </ng-template>
        </div>
    
        <div class="message is-warning" *ngIf="isLocked() && !isCreationMode()">
            <div class="message-body">
                <s5l *ngIf="currentTab === PROPERTIES_TAB">services.connector.locked.warning.properties</s5l>
                <s5l *ngIf="currentTab === ASSIGNMENT_TAB">services.connector.locked.warning.assignment</s5l>
            </div>
        </div>

        <div class="message is-warning" 
            *ngIf="isInherited() && !isCreationMode() && currentTab === PROPERTIES_TAB">
            <div class="message-body">
                <s5l>services.connector.inherited.warning.properties</s5l>
            </div>
        </div>

        <div class="tabs" *ngIf="!isCreationMode()">
            <button class="tab"
                    [ngClass]="{active: currentTab === PROPERTIES_TAB}"
                    (click)="currentTab = PROPERTIES_TAB">
                {{ 'services.tab.properties' | translate }}
            </button>
            <button class="tab"
                    [ngClass]="{active: currentTab === ASSIGNMENT_TAB}"
                    (click)="currentTab = ASSIGNMENT_TAB">
                {{ 'services.tab.assignment' | translate }}
            </button>
        </div>

        <connector-properties
            *ngIf="currentTab === PROPERTIES_TAB"
            [connector]="servicesStore.connector"
            [casTypes]="casTypes"
            [structureChildren]="hasStructureChildren()"
            [creationMode]="isCreationMode()"
            [disabled]="arePropertiesDisabled()"
            (create)="onCreate($event)">
        </connector-properties>

        <connector-assignment
            *ngIf="currentTab === ASSIGNMENT_TAB"
            [connector]="servicesStore.connector"
            [disabled]="isAssignmentDisabled()"
            (remove)="onRemoveAssignment($event)"
            (add)="onAddAssignment($event)">
        </connector-assignment>

        <lightbox-confirm lightboxTitle="services.connector.delete.confirm.title"
                          [show]="showDeleteConfirmation"
                          (onCancel)="showDeleteConfirmation = false;"
                          (onConfirm)="onConfirmDeletion()">
            <div class="has-vertical-margin-10">
                <span [innerHTML]="'services.connector.delete.confirm.content' | translate: {connector: servicesStore.connector.displayName}"></span>
            </div>
        </lightbox-confirm>
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
        button.tab:hover {
            color: #ff8352;
            background-color: #fff;
            border-bottom-color: #ff8352;
        }
    `]
})
export class SmartConnectorComponent implements OnInit, OnDestroy {
    public casTypes: CasType[];
    private routeSubscription: Subscription;
    private rolesSubscription: Subscription;
    private casTypesSubscription: Subscription;
    public admc: boolean;
    public admlOfConnectorStructure: boolean;
    public showDeleteConfirmation: boolean;
    
    public PROPERTIES_TAB = 'properties';
    public ASSIGNMENT_TAB = 'assignment';
    public MASS_ASSIGNEMENT_TAB = 'massAssignment';
    public currentTab: string = this.PROPERTIES_TAB;

    constructor(private servicesService: ServicesService,
                private activatedRoute: ActivatedRoute,
                public servicesStore: ServicesStore,
                private spinnerService: SpinnerService,
                private notifyService: NotifyService,
                private router: Router,
                private location: Location,
                private bundles: BundlesService) {
    }

    ngOnInit() {
        this.routeSubscription = this.activatedRoute.params.subscribe((params: Params) => {
            if (params['connectorId']) {
                this.servicesStore.connector = this.servicesStore.structure
                    .connectors.data.find(a => a.id === params['connectorId']);
            } else {
                this.servicesStore.connector = new ConnectorModel();
            }
        });

        this.rolesSubscription = this.activatedRoute.data.subscribe(data => {
            if(data["roles"]) {
                this.servicesStore.connector.roles = data["roles"];
                // Hack to gracful translate connector's role's name
                this.servicesStore.connector.roles.forEach(r => {
                    r.name = `${this.servicesStore.connector.name} - ${this.bundles.translate('services.connector.access')}`;
                });
            }
        })

        this.casTypesSubscription = this.servicesService
            .getCasTypes()
            .subscribe((res:CasType[]) => this.casTypes = res);

        this.setAdmc();
        this.setAdmlOfConnectorStructure();
    }

    ngOnDestroy() {
        this.routeSubscription.unsubscribe();
        this.rolesSubscription.unsubscribe();
        this.casTypesSubscription.unsubscribe();
    }

    public async setAdmlOfConnectorStructure() {
        const session: Session = await SessionModel.getSession();
        if (session.functions && session.functions['ADMIN_LOCAL'] && session.functions['ADMIN_LOCAL'].scope) {
            this.admlOfConnectorStructure = session.functions['ADMIN_LOCAL'].scope.includes(this.servicesStore.connector.structureId);
        }
    }

    public async setAdmc() {
        const session: Session = await SessionModel.getSession();
        this.admc = session.functions && session.functions['SUPER_ADMIN'] != null;
    }

    public hasStructureChildren(): boolean {
        return this.servicesStore.structure.children 
            &&  this.servicesStore.structure.children.length > 0;
    }

    public isCreationMode(): boolean {
        return this.servicesStore.connector && !this.servicesStore.connector.id;
    }

    public arePropertiesDisabled(): boolean {
        return (this.isLocked() 
            || !(this.admc || this.admlOfConnectorStructure)
            || this.isInherited())
            && !this.isCreationMode();
    }

    public isAssignmentDisabled(): boolean {
        return this.isLocked() || !(this.admc || this.admlOfConnectorStructure);
    }

    public isInherited(): boolean {
        return this.servicesStore.connector 
            && this.servicesStore.connector.inherits 
            && this.servicesStore.connector.structureId !== this.servicesStore.structure.id;
    }

    public isLocked(): boolean {
        return this.servicesStore.connector && this.servicesStore.connector.locked;
    }

    public onCreate($event): void {
        if($event === 'submit') {
            this.spinnerService.perform('portal-content'
                , this.servicesService.createConnector(this.servicesStore.connector, this.servicesStore.structure.id)
                    .do(res => {
                        this.servicesStore.connector.id = res.id;
                        this.servicesStore.connector.structureId = this.servicesStore.structure.id;
                        this.servicesStore.structure.connectors.data.push(this.servicesStore.connector);
                        this.notifyService.success({
                            key: 'services.connector.create.success.content',
                            parameters: {connector: this.servicesStore.connector.displayName}
                        }, 'services.connector.create.success.title');
        
                        this.router.navigate(['..', res.id]
                            , {relativeTo: this.activatedRoute, replaceUrl: false});
                    })
                    .catch(error => {
                        if (error.error && error.error.error) {
                            this.notifyService.error(error.error.error
                                , 'services.connector.create.error.title'
                                , error);
                        } else {
                            this.notifyService.error({
                                key: 'services.connector.create.error.content'
                                , parameters: {connector: this.servicesStore.connector.displayName}
                            }, 'services.connector.create.error.title'
                            , error);
                        }
                        throw error;
                    })
                    .toPromise()
                );
        } else if ($event === 'cancel') {
            this.location.back();
        } else {
            console.error('unknown event from create EventEmitter');
        }
    }

    public save(): void {
        this.spinnerService.perform('portal-content'
            , this.servicesService.saveConnector(this.servicesStore.connector, this.servicesStore.structure.id)
                .do(res => {
                    this.notifyService.success({
                        key: 'services.connector.save.success.content',
                        parameters: {connector: this.servicesStore.connector.displayName}
                    }, 'services.connector.save.success.title');
                })
                .catch(error => {
                    if (error.error && error.error.error) {
                        this.notifyService.error(error.error.error
                            , 'services.connector.save.error.title'
                            , error);
                    } else {
                        this.notifyService.error({
                            key: 'services.connector.save.error.content'
                            , parameters: {connector: this.servicesStore.connector.displayName}
                        }, 'services.connector.save.error.title'
                        , error);
                    }
                    throw error;
                })
                .toPromise()
            );
    }

    public onConfirmDeletion() {
        this.spinnerService.perform('portal-content'
            , this.servicesService.deleteConnector(this.servicesStore.connector)
                .do(() => {
                    this.servicesStore.structure.connectors.data.splice(
                        this.servicesStore.structure.connectors.data.findIndex(c => c == this.servicesStore.connector)
                        , 1);

                    this.notifyService.success({
                        key: 'services.connector.delete.success.content',
                        parameters: {connector: this.servicesStore.connector.displayName}
                    }, 'services.connector.delete.success.title');

                    this.router.navigate(['..'], {relativeTo: this.activatedRoute, replaceUrl: false});
                })
                .catch(error => {
                    this.notifyService.error({
                        key: 'services.connector.delete.error.content'
                        , parameters: {connector: this.servicesStore.connector.displayName}
                    }, 'services.connector.delete.error.title'
                    , error);
                    throw error;
                })
                .toPromise()
        );
    }

    public lockToggle() {
        this.spinnerService.perform('portal-content',
            this.servicesService.toggleLockConnector(this.servicesStore.connector)
                .do(() => {
                    this.servicesStore.connector.locked = !this.servicesStore.connector.locked;

                    let notifySuccessTitle: string = '';
                    let notifySuccessContent: string = '';

                    if (this.servicesStore.connector.locked) {
                        notifySuccessTitle = 'services.connector.lock.success.title';
                        notifySuccessContent = 'services.connector.lock.success.content';
                    } else {
                        notifySuccessTitle = 'services.connector.unlock.success.title';
                        notifySuccessContent = 'services.connector.unlock.success.content';
                    }

                    this.notifyService.success({
                        key: notifySuccessContent,
                        parameters: {connector: this.servicesStore.connector.displayName}
                    }, notifySuccessTitle);
                })
                .catch(error => {
                    let notifyErrorTitle: string = '';
                    let notifyErrorContent: string = '';

                    if (this.servicesStore.connector.locked) {
                        notifyErrorTitle = 'services.connector.lock.error.title';
                        notifyErrorContent = 'services.connector.lock.error.content';
                    } else {
                        notifyErrorTitle = 'services.connector.unlock.error.title';
                        notifyErrorContent = 'services.connector.unlock.error.content';
                    }

                    this.notifyService.error({
                        key: notifyErrorContent
                        , parameters: {connector: this.servicesStore.connector.displayName}
                    }, notifyErrorTitle, error);
                    throw error;
                })
                .toPromise()
        );
    }

    public onAddAssignment($event: {group: GroupModel, role: RoleModel}) {
        $event.role.addGroup($event.group);
    }

    public onRemoveAssignment($event: {group: GroupModel, role: RoleModel}): void {
        $event.role.removeGroup($event.group);
    }
}