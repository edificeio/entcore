import { Location } from '@angular/common';
import { Component, Injector, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Data, Params } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { BundlesService } from 'ngx-ode-sijil';
import { SpinnerService } from 'ngx-ode-ui';
import { catchError, tap } from 'rxjs/operators';
import { NotifyService } from 'src/app/core/services/notify.service';
import { routing } from 'src/app/core/services/routing.service';
import { Session } from 'src/app/core/store/mappings/session';
import { ConnectorModel } from 'src/app/core/store/models/connector.model';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { SessionModel } from 'src/app/core/store/models/session.model';
import { ServicesService, WorkspaceDocument } from '../../../services.service';
import { ServicesStore } from '../../../services.store';
import { Assignment, Profile } from '../../../_shared/services-types';
import { CasType } from '../CasType';
import { ExportFormat } from '../export/connector-export.component';
import { ConnectorPropertiesComponent } from '../properties/connector-properties.component';

@Component({
    selector: 'ode-smart-connector',
    templateUrl: './smart-connector.component.html'
})
export class SmartConnectorComponent extends OdeComponent implements OnInit, OnDestroy {
    public casTypes: CasType[];

    public admc: boolean;
    public admlOfConnectorStructure: boolean;
    public admlOfCurrentStructure: boolean;
    public showDeleteConfirmation: boolean;
    public profiles: Array<Profile> = ['Guest', 'Personnel', 'Relative', 'Student', 'Teacher', 'AdminLocal'];
    public assignmentGroupPickerList: GroupModel[];

    @ViewChild(ConnectorPropertiesComponent, { static: false })
    connectorPropertiesComponent: ConnectorPropertiesComponent;

    public PROPERTIES_TAB = 'properties';
    public ASSIGNMENT_TAB = 'assignment';
    public MASS_ASSIGNMENT_TAB = 'massAssignment';
    public EXPORT_TAB = 'export';
    public currentTab: string = this.PROPERTIES_TAB;

    constructor(private servicesService: ServicesService,
                injector: Injector,
                public servicesStore: ServicesStore,
                private spinnerService: SpinnerService,
                private notifyService: NotifyService,
                private location: Location,
                private bundles: BundlesService) {
                  super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.route.params.subscribe((params: Params) => {
            if (params.connectorId) {
                this.servicesStore.connector = this.servicesStore.structure
                    .connectors.data.find(a => a.id === params.connectorId);
            } else {
                this.servicesStore.connector = new ConnectorModel();
            }
        }));

        this.subscriptions.add(this.route.data.subscribe(data => {
            if (data.roles) {
                this.servicesStore.connector.roles = data.roles;
                // Hack to gracful translate connector's role's name
                this.servicesStore.connector.roles.forEach(r => {
                    r.name = `${this.servicesStore.connector.name} - ${this.bundles.translate('services.connector.access')}`;
                });
            }
        }));

        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.assignmentGroupPickerList = this.servicesStore.structure.groups.data;
                if (!this.hasStructureChildren() && this.currentTab === this.MASS_ASSIGNMENT_TAB) {
                    this.currentTab = this.PROPERTIES_TAB;
                }
            }
        }));

        this.subscriptions.add(this.servicesService
            .getCasTypes()
            .subscribe((res: CasType[]) => this.casTypes = res));

        this.setAdmc();
        this.setAdmlOfConnectorStructure();
        this.setAdmlOfCurrentStructure();
    }

  
    public async setAdmlOfConnectorStructure() {
        const session: Session = await SessionModel.getSession();
        if (session.functions && session.functions.ADMIN_LOCAL && session.functions.ADMIN_LOCAL.scope) {
            this.admlOfConnectorStructure = session.functions.ADMIN_LOCAL.scope.includes(this.servicesStore.connector.structureId);
        }
    }

    public async setAdmlOfCurrentStructure() {
        const session: Session = await SessionModel.getSession();
        if (session.functions && session.functions.ADMIN_LOCAL && session.functions.ADMIN_LOCAL.scope) {
            this.admlOfCurrentStructure = session.functions.ADMIN_LOCAL.scope.includes(this.servicesStore.structure.id);
        }
    }

    public async setAdmc() {
        const session: Session = await SessionModel.getSession();
        this.admc = session.isADMC();
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
        if ($event === 'submit') {
            this.spinnerService.perform('portal-content'
                , this.servicesService.createConnector(this.servicesStore.connector, this.servicesStore.structure.id)
                .pipe(
                  tap(res => {
                    this.servicesStore.connector.id = res.id;
                    this.servicesStore.connector.structureId = this.servicesStore.structure.id;
                    this.servicesStore.structure.connectors.data.push(this.servicesStore.connector);
                    this.notifyService.success({
                      key: 'services.connector.create.success.content',
                      parameters: {connector: this.servicesStore.connector.displayName}
                    }, 'services.connector.create.success.title');

                    this.router.navigate(['..', res.id]
                      , {relativeTo: this.route, replaceUrl: false});
                  }),
                  catchError(
                    error => {
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
                    }
                  )
                ).toPromise());
        } else if ($event === 'cancel') {
            this.location.back();
        } else {
            console.error('unknown event from create EventEmitter');
        }
    }

    public save(): void {
        this.spinnerService.perform('portal-content'
            , this.servicesService.saveConnector(this.servicesStore.connector, this.servicesStore.structure.id)
            .pipe(
              tap(res => {
                this.connectorPropertiesComponent.propertiesFormRef.form.markAsPristine();
                this.notifyService.success({
                  key: 'services.connector.save.success.content',
                  parameters: {connector: this.servicesStore.connector.displayName}
                }, 'services.connector.save.success.title');
              }),
              catchError(error => {
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
            ).toPromise());
    }

    public onConfirmDeletion(): void {
        this.spinnerService.perform('portal-content'
            , this.servicesService.deleteConnector(this.servicesStore.connector)
            .pipe(
              tap(() => {
                this.servicesStore.structure.connectors.data.splice(
                  this.servicesStore.structure.connectors.data.findIndex(c => c === this.servicesStore.connector)
                  , 1);

                this.notifyService.success({
                  key: 'services.connector.delete.success.content',
                  parameters: {connector: this.servicesStore.connector.displayName}
                }, 'services.connector.delete.success.title');

                this.router.navigate(['..'], {relativeTo: this.route, replaceUrl: false});
              }),
              catchError(
                error => {
                  this.notifyService.error({
                      key: 'services.connector.delete.error.content'
                      , parameters: {connector: this.servicesStore.connector.displayName}
                    }, 'services.connector.delete.error.title'
                    , error);
                  throw error;
                }
              )
            ).toPromise());
    }

    public lockToggle(): void {
        this.spinnerService.perform('portal-content',
            this.servicesService.toggleLockConnector(this.servicesStore.connector)
              .pipe(
                tap(() => {
                  this.servicesStore.connector.locked = !this.servicesStore.connector.locked;

                  let notifySuccessTitle = '';
                  let notifySuccessContent = '';

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
                }),
                catchError(
                  error => {
                    let notifyErrorTitle = '';
                    let notifyErrorContent = '';

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
                  }
                )
              ).toPromise());
    }

    public onAddAssignment(assignment: Assignment): void {
        assignment.role.addGroup(assignment.group);
    }

    public onRemoveAssignment(assignment: Assignment): void {
        assignment.role.removeGroup(assignment.group);
    }

    public onAddMassAssignment(profiles: Array<Profile>): void {
        this.spinnerService.perform('portal-content',
            this.servicesService.massAssignConnector(this.servicesStore.structure.id, this.servicesStore.connector, profiles)
              .pipe(

                tap(() => {
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
                }),
                catchError(
                  error => {
                    this.notifyService.error(
                      'services.connector.mass-assignment.assign-error.content',
                      'services.connector.mass-assignment.assign-error.title'
                      , error);
                    throw error;
                  }
                )
              ).toPromise());
    }

    public onRemoveMassAssignment(profiles: Array<Profile>): void {
        this.spinnerService.perform('portal-content',
            this.servicesService.massUnassignConnector(this.servicesStore.structure.id, this.servicesStore.connector, profiles)
              .pipe(
                tap(() => {
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
                }),
                catchError(error => {
                  this.notifyService.error(
                    'services.connector.mass-assignment.unassign-error.content',
                    'services.connector.mass-assignment.unassign-error.title'
                    , error);
                  throw error;
                })
              ).toPromise());
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
              .pipe(
                tap((res: WorkspaceDocument) => {
                  this.servicesStore.connector.icon = `/workspace/document/${res._id}`;
                  this.connectorPropertiesComponent.propertiesFormRef.form.markAsDirty();
                }),
                catchError(
                  error => {
                    this.notifyService.error('services.connector.icon.upload.error.content'
                      , 'services.connector.icon.upload.error.title'
                      , error);
                    throw error;
                  }
                )
              ).toPromise());
    }

    public onIconFileInvalid($event: string): void {
        this.notifyService.error('services.connector.icon.upload.error.content'
            , 'services.connector.icon.upload.error.title'
            , { message: $event });
    }
}
