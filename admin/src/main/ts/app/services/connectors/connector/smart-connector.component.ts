import { Component, OnInit, OnDestroy, ViewChild } from "@angular/core";
import { Subscription } from "rxjs";
import { CasType } from "./CasType";
import { ConnectorModel, Session, SessionModel, GroupModel, globalStore } from "../../../core/store";
import { ServicesService, WorkspaceDocument } from "../../services.service";
import { ActivatedRoute, Params, Router, Data } from "@angular/router";
import { ServicesStore } from "../../services.store";
import { SpinnerService, NotifyService, routing } from "../../../core/services";
import { Location } from "@angular/common";
import { BundlesService } from "sijil";
import { ConnectorPropertiesComponent } from "./properties/connector-properties.component";
import { Profile, Assignment } from "../../shared/services-types";
import { ExportFormat } from "./export/connector-export";

import 'rxjs/add/operator/do';
import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/toPromise';

@Component({
    selector: 'smart-connector',
    template: `
        <div class="panel-header is-display-flex has-align-items-center has-space-between">
            <span *ngIf="isCreationMode() else isEditionMode">
                <s5l>services.connector.create.title</s5l>
            </span>
            <ng-template #isEditionMode>
                <div>
                    <span>{{ servicesStore.connector.displayName }}</span>
                    <i *ngIf="isInherited()"
                        class="fa fa-link has-left-margin-5"
                        [title]="'services.connector.inherited' | translate">
                    </i>
                    <i *ngIf="isLocked()"
                        class="fa fa-lock has-left-margin-5"
                        [title]="'services.connector.locked' | translate">
                    </i>
                </div>

                <div>
                    <button type="button"
                            *ngIf="!isInherited() && !isLocked()"
                            (click)="showDeleteConfirmation = true;"
                            [disabled]="arePropertiesDisabled()">
                        <s5l>services.connector.delete.button</s5l>
                        <i class="fa fa-trash is-size-5"></i>
                    </button>

                    <button type="button"
                            *ngIf="admc && !isInherited()"
                            (click)="lockToggle()">
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

                    <button type="button" 
                        class="confirm"
                        (click)="save()"
                        *ngIf="currentTab === PROPERTIES_TAB && !isInherited() && !isLocked()"
                        [disabled]="arePropertiesDisabled() || isSaveFormPristineOrInvalid()">
                        <s5l>services.connector.save.button</s5l>
                        <i class="fa fa-floppy-o is-size-5"></i>
                    </button>
                </div>
            </ng-template>
        </div>
    
        <div class="message is-warning" 
            *ngIf="isLocked() 
                && !isInherited() 
                && !isCreationMode() 
                && currentTab === PROPERTIES_TAB">
            <div class="message-body">
                <s5l>services.connector.locked.warning.properties</s5l>
            </div>
        </div>
        
        <div class="message is-warning" 
            *ngIf="isInherited() 
                && !isCreationMode() 
                && currentTab === PROPERTIES_TAB">
            <div class="message-body">
                <s5l>services.connector.inherited.warning.properties</s5l>
            </div>
        </div>

        <div class="message is-warning"
            *ngIf="isLocked()
                && !isCreationMode()
                && currentTab === ASSIGNMENT_TAB">
            <div class="message-body">
                <s5l *ngIf="currentTab === ASSIGNMENT_TAB">
                    services.connector.locked.warning.assignment
                </s5l>
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
            <button class="tab"
                    *ngIf="hasStructureChildren() && !isMassAssignmentDisabled() && inherits()"
                    [ngClass]="{active: currentTab === MASS_ASSIGNMENT_TAB}"
                    (click)="currentTab = MASS_ASSIGNMENT_TAB">
                {{ 'services.tab.mass-assignment' | translate }}
            </button>
            <button class="tab"
                    [ngClass]="{active: currentTab === EXPORT_TAB}"
                    (click)="currentTab = EXPORT_TAB">
                {{ 'services.tab.export' | translate }}
            </button>
        </div>

        <connector-properties
            *ngIf="currentTab === PROPERTIES_TAB"
            [connector]="servicesStore.connector"
            [casTypes]="casTypes"
            [structureChildren]="hasStructureChildren()"
            [creationMode]="isCreationMode()"
            [disabled]="arePropertiesDisabled()"
            [admc]="admc"
            (create)="onCreate($event)"
            (iconFileChanged)="onIconFileChanged($event)"
            (iconFileInvalid)="onIconFileInvalid($event)">
        </connector-properties>

        <connector-assignment
            *ngIf="currentTab === ASSIGNMENT_TAB"
            [connector]="servicesStore.connector"
            [assignmentGroupPickerList]="assignmentGroupPickerList"
            [disabled]="isAssignmentDisabled()"
            (remove)="onRemoveAssignment($event)"
            (add)="onAddAssignment($event)">
        </connector-assignment>

        <connector-mass-assignment
            *ngIf="currentTab === MASS_ASSIGNMENT_TAB && !isMassAssignmentDisabled()"
            [structure]="{ id: this.servicesStore.structure.id, name: this.servicesStore.structure.name }"
            [profiles]="profiles"
            (submitUnassignment)="onRemoveMassAssignment($event)"
            (submitAssignment)="onAddMassAssignment($event)">
        </connector-mass-assignment>

        <connector-export
            *ngIf="currentTab === EXPORT_TAB"
            (submit)="onExportSubmit($event)">
        </connector-export>

        <lightbox-confirm lightboxTitle="services.connector.delete.confirm.title"
                          [show]="showDeleteConfirmation"
                          (onCancel)="showDeleteConfirmation = false;"
                          (onConfirm)="onConfirmDeletion()">
            <span [innerHTML]="'services.connector.delete.confirm.content' | translate: {connector: servicesStore.connector.displayName}"></span>
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
    public admlOfCurrentStructure: boolean;
    public showDeleteConfirmation: boolean;
    public profiles: Array<Profile> = ['Guest', 'Personnel', 'Relative', 'Student', 'Teacher', 'AdminLocal'];
    private structureSubscriber: Subscription;
    public assignmentGroupPickerList: GroupModel[];
    
    @ViewChild(ConnectorPropertiesComponent)
    connectorPropertiesComponent: ConnectorPropertiesComponent;
    
    public PROPERTIES_TAB: string = 'properties';
    public ASSIGNMENT_TAB: string = 'assignment';
    public MASS_ASSIGNMENT_TAB: string = 'massAssignment';
    public EXPORT_TAB: string = 'export';
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
        });

        this.structureSubscriber = routing.observe(this.activatedRoute, 'data').subscribe((data: Data) => {
            if (data['structure']) {
                this.assignmentGroupPickerList = this.servicesStore.structure.groups.data;
                if (!this.hasStructureChildren() && this.currentTab === this.MASS_ASSIGNMENT_TAB) {
                    this.currentTab = this.PROPERTIES_TAB;
                }
            }
        });

        this.casTypesSubscription = this.servicesService
            .getCasTypes()
            .subscribe((res:CasType[]) => this.casTypes = res);

        this.setAdmc();
        this.setAdmlOfConnectorStructure();
        this.setAdmlOfCurrentStructure();
    }

    ngOnDestroy() {
        this.routeSubscription.unsubscribe();
        this.rolesSubscription.unsubscribe();
        this.casTypesSubscription.unsubscribe();
        this.structureSubscriber.unsubscribe();
    }

    public async setAdmlOfConnectorStructure() {
        const session: Session = await SessionModel.getSession();
        if (session.functions && session.functions['ADMIN_LOCAL'] && session.functions['ADMIN_LOCAL'].scope) {
            this.admlOfConnectorStructure = session.functions['ADMIN_LOCAL'].scope.includes(this.servicesStore.connector.structureId);
        }
    }

    public async setAdmlOfCurrentStructure() {
        const session: Session = await SessionModel.getSession();
        if (session.functions && session.functions['ADMIN_LOCAL'] && session.functions['ADMIN_LOCAL'].scope) {
            this.admlOfCurrentStructure = session.functions['ADMIN_LOCAL'].scope.includes(this.servicesStore.structure.id);
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
        return this.isLocked() || !(this.admc || this.admlOfCurrentStructure);
    }

    public isMassAssignmentDisabled(): boolean {
        return this.isLocked() || !(this.admc || this.admlOfConnectorStructure);
    }

    public isInherited(): boolean {
        return this.servicesStore.connector 
            && this.servicesStore.connector.inherits 
            && this.servicesStore.connector.structureId !== this.servicesStore.structure.id;
    }

    public inherits(): boolean {
        return this.servicesStore.connector && this.servicesStore.connector.inherits;
    }

    public isLocked(): boolean {
        return this.servicesStore.connector && this.servicesStore.connector.locked;
    }

    public isSaveFormPristineOrInvalid(): boolean {
        return this.connectorPropertiesComponent 
            && (this.connectorPropertiesComponent.propertiesFormRef.pristine 
                || this.connectorPropertiesComponent.propertiesFormRef.invalid);
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
                    this.connectorPropertiesComponent.propertiesFormRef.form.markAsPristine();
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

    public onConfirmDeletion(): void {
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

    public lockToggle(): void {
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

    public onAddAssignment(assignment: Assignment): void {
        assignment.role.addGroup(assignment.group);
    }

    public onRemoveAssignment(assignment: Assignment): void {
        assignment.role.removeGroup(assignment.group);
    }

    public onAddMassAssignment(profiles: Array<Profile>): void {
        this.spinnerService.perform('portal-content', 
            this.servicesService.massAssignConnector(this.servicesStore.connector, profiles)
                .do(() => {
                    this.servicesStore.connector
                        .syncRoles(this.servicesStore.structure.id, this.servicesStore.connector.id)
                        .then(() => {
                            this.servicesStore.connector.roles
                                .forEach(r => {
                                    r.name = `${this.servicesStore.connector.name} - ${this.bundles.translate('services.connector.access')}`;
                                });
                        });

                    this.notifyService.success(
                        'services.connector.mass-assignment.assign-success.content',
                        'services.connector.mass-assignment.assign-success.title');
                })
                .catch(error => {
                    this.notifyService.error(
                        'services.connector.mass-assignment.assign-error.content',
                        'services.connector.mass-assignment.assign-error.title'
                        , error);
                    throw error;
                })
                .toPromise()
        );
    }

    public onRemoveMassAssignment(profiles: Array<Profile>): void {
        this.spinnerService.perform('portal-content', 
            this.servicesService.massUnassignConnector(this.servicesStore.connector, profiles)
                .do(() => {
                    this.servicesStore.connector
                        .syncRoles(this.servicesStore.structure.id, this.servicesStore.connector.id)
                        .then(() => {
                            this.servicesStore.connector.roles
                                .forEach(r => {
                                    r.name = `${this.servicesStore.connector.name} - ${this.bundles.translate('services.connector.access')}`;
                                });
                        });

                    this.notifyService.success(
                        'services.connector.mass-assignment.unassign-success.content',
                        'services.connector.mass-assignment.unassign-success.title');
                })
                .catch(error => {
                    this.notifyService.error(
                        'services.connector.mass-assignment.unassign-error.content',
                        'services.connector.mass-assignment.unassign-error.title'
                        , error);
                    throw error;
                })
                .toPromise()
        );
    }

    public onExportSubmit($event: {exportFormat: ExportFormat, profile: string}): void {
        window.open(
            this.servicesService.getExportConnectorUrl(
                $event.exportFormat, $event.profile, this.servicesStore.structure.id)
            , '_blank');
    }

    public onIconFileChanged($event: File[]): void {
        const file: Blob = $event[0];
        
        this.spinnerService.perform('portal-content', 
            this.servicesService.uploadPublicImage(file)
                .do((res: WorkspaceDocument) => {
                    this.servicesStore.connector.icon = `/workspace/document/${res._id}`;
                    this.connectorPropertiesComponent.propertiesFormRef.form.markAsDirty();
                })
                .catch(error => {
                    this.notifyService.error('services.connector.icon.upload.error.content'
                        , 'services.connector.icon.upload.error.title'
                        , error);
                    throw error;
                })
                .toPromise()
        );
    }

    public onIconFileInvalid($event: string): void {
        this.notifyService.error('services.connector.icon.upload.error.content'
            , 'services.connector.icon.upload.error.title'
            , { message: $event });
    }
}