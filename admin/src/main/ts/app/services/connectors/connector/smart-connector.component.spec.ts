import { SmartConnectorComponent } from './smart-connector.component';
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { FormsModule, NgForm, FormBuilder } from '@angular/forms';
import { SijilModule, BundlesService } from 'sijil';
import { UxModule } from '../../../shared/ux/ux.module';
import { ServicesStore } from '../../services.store';
import { NotifyService, SpinnerService } from '../../../core/services';
import { StructureModel, ConnectorModel, ConnectorCollection } from '../../../core/store';
import { Router, ActivatedRoute } from '@angular/router';
import { ServicesService } from '../../services.service';
import { Observable } from 'rxjs';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CasType } from './CasType';
import { Structure, Profile, Assignment } from '../../shared/services-types';
import { ExportFormat } from './export/connector-export';
import { ConnectorPropertiesComponent } from './properties/connector-properties.component';

describe('SmartConnector', () => {
    let component: SmartConnectorComponent;
    let fixture: ComponentFixture<SmartConnectorComponent>;
    let mockServicesService: ServicesService;
    let mockServicesStore: ServicesStore;
    let mockNotifyService: NotifyService;
    let mockSpinnerService: SpinnerService;
    let mockRouter: Router;
    let mockLocation: Location;
    let mockBundle: BundlesService;
    let mockActivatedRoute: ActivatedRoute;
    let connectorsDataPushSpy: jasmine.Spy;
    let connectorsDataSpliceSpy: jasmine.Spy;

    beforeEach(() => {
        mockServicesService = jasmine.createSpyObj('ServicesService', ['createConnector', 'saveConnector', 'deleteConnector', 'toggleLockConnector', 'getCasTypes', 'massAssignConnector', 'massUnassignConnector', 'getExportConnectorUrl', 'uploadPublicImage']);
        (mockServicesService.getCasTypes as jasmine.Spy).and.returnValue(Observable.of([
            {id: 'casType1', name:'casType1', description: 'casType1s'}
        ]));

        mockActivatedRoute = {
            data: Observable.of({
                roles: [
                    { id: 'myRole1', name: 'myRole1', transverse: false },
                    { id: 'myRole2', name: 'myRole2', transverse: false },
                    { id: 'myRole3', name: 'myRole3', transverse: true }
                ]
            }),
            params: Observable.of({ connectorId: 'connector1' }),
            pathFromRoot: [{
                data: Observable.of({
                    structure: {children: []}
                })
            }]
        } as ActivatedRoute;

        mockServicesStore = new ServicesStore();
        mockServicesStore.structure = new StructureModel();
        mockServicesStore.structure.id = 'structure1';
        mockServicesStore.structure.connectors = new ConnectorCollection();
        mockServicesStore.connector = new ConnectorModel();
        mockServicesStore.connector.id = 'connector1';

        mockServicesStore.structure.connectors.data = [mockServicesStore.connector];
        mockNotifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        mockSpinnerService = jasmine.createSpyObj('SpinnerService', ['perform']);
        mockRouter = jasmine.createSpyObj('Router', ['navigate']);
        mockLocation = jasmine.createSpyObj('Location', ['back']);
        mockBundle = jasmine.createSpyObj('BundlesService', ['translate']);
    });

    beforeEach(async(() => {
       TestBed.configureTestingModule({
            declarations: [
               SmartConnectorComponent, 
               MockConnectorPropertiesComponent, 
               MockConnectorAssignmentComponent,
               MockConnectorMassAssignmentComponent,
               MockConnectorExport
            ],
            providers: [
               {provide: ServicesService, useValue: mockServicesService},
               {provide: ServicesStore, useValue: mockServicesStore},
               {provide: NotifyService, useValue: mockNotifyService},
               {provide: SpinnerService, useValue: mockSpinnerService},
               {provide: Router, useValue: mockRouter},
               {provide: ActivatedRoute, useValue: mockActivatedRoute},
               {provide: Location, useValue: mockLocation},
               {provide: BundlesService, useValue: mockBundle}
            ],
            imports: [
               SijilModule.forRoot(),
               FormsModule,
               UxModule.forRoot(null)
            ]
       }).compileComponents();

       fixture = TestBed.createComponent(SmartConnectorComponent);
       component = fixture.debugElement.componentInstance;
       component.connectorPropertiesComponent = TestBed.createComponent(MockConnectorPropertiesComponent).componentInstance as ConnectorPropertiesComponent;
       fixture.detectChanges();
    }));

    it('should create the SmartConnectorComponent component', async(() => {
        expect(component).toBeTruthy();
    }));

    describe('onCreate', () => {
        it('should create a new connector when given sumbit event', () => {
            (mockServicesService.createConnector as jasmine.Spy).and.returnValue(Observable.of({id: 'newConnectorId', roleId: 'newRoleId'}));
            connectorsDataPushSpy = spyOn(mockServicesStore.structure.connectors.data, 'push');

            component.onCreate('submit');
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            expect(mockServicesService.createConnector).toHaveBeenCalled();
            expect(mockServicesStore.connector.id).toBe('newConnectorId');
            expect(mockServicesStore.connector.structureId).toBe(mockServicesStore.structure.id);
            const pushedConnector: ConnectorModel = connectorsDataPushSpy.calls.mostRecent().args[0];
            expect(pushedConnector.id).toBe('newConnectorId');
            expect(mockNotifyService.success).toHaveBeenCalled();
            expect(mockRouter.navigate).toHaveBeenCalledWith(['..', pushedConnector.id]
                , {relativeTo: mockActivatedRoute, replaceUrl: false});
        });

        it('should call navigation.back when given cancel event', () => {
            component.onCreate('cancel');
            expect(mockLocation.back).toHaveBeenCalled();
        });
    });

    describe('save', () => {
        it('should save connector', () => {
            (mockServicesService.saveConnector as jasmine.Spy).and.returnValue(Observable.of({}));

            component.connectorPropertiesComponent.propertiesFormRef = <NgForm>{};
            component.connectorPropertiesComponent.propertiesFormRef.form = new FormBuilder().group({});
            spyOn(component.connectorPropertiesComponent.propertiesFormRef.form, 'markAsPristine');

            component.save();
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            expect(mockServicesService.saveConnector).toHaveBeenCalled();
            expect(component.connectorPropertiesComponent.propertiesFormRef.form.markAsPristine).toHaveBeenCalled();
            expect(mockNotifyService.success).toHaveBeenCalled();
        });
    });

    describe('onConfirmDeletion', () => {
        it('should delete connector', () => {
            (mockServicesService.deleteConnector as jasmine.Spy).and.returnValue(Observable.of({}));
            connectorsDataSpliceSpy = spyOn(mockServicesStore.structure.connectors.data, 'splice');
            const connectorToDelete: ConnectorModel = new ConnectorModel();
            connectorToDelete.id = 'connectorToDeleteId';
            mockServicesStore.structure.connectors.data.push(connectorToDelete);
            mockServicesStore.connector = connectorToDelete;

            component.onConfirmDeletion();
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            expect(mockServicesService.deleteConnector).toHaveBeenCalled();
            const splicedConnectorIndex: number = connectorsDataSpliceSpy.calls.mostRecent().args[0];
            
            expect(splicedConnectorIndex).toBe(mockServicesStore.structure.connectors.data.length - 1);
            expect(mockNotifyService.success).toHaveBeenCalled();
            expect(mockRouter.navigate).toHaveBeenCalledWith(['..']
                , {relativeTo: mockActivatedRoute, replaceUrl: false});
        });
    });

    describe('lockToggle', () => {
        it('should lock connector', () => {
            mockServicesStore.connector.locked = false;
            (mockServicesService.toggleLockConnector as jasmine.Spy).and.returnValue(Observable.of({}));

            component.lockToggle();
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            expect(mockServicesService.toggleLockConnector).toHaveBeenCalled();
            expect(mockServicesStore.connector.locked).toBe(true);
            expect(mockNotifyService.success).toHaveBeenCalled();
        });

        it('should unlock connector', () => {
            mockServicesStore.connector.locked = true;
            (mockServicesService.toggleLockConnector as jasmine.Spy).and.returnValue(Observable.of({}));

            component.lockToggle();
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            expect(mockServicesService.toggleLockConnector).toHaveBeenCalled();
            expect(mockServicesStore.connector.locked).toBe(false);
            expect(mockNotifyService.success).toHaveBeenCalled();
        });
    });

    describe('onAddMassAssignment', () => {
        it('should add mass assignment for given profiles', () => {
            const profiles: Profile[] = ['Personnel'];
            (mockServicesService.massAssignConnector as jasmine.Spy).and.returnValue(Observable.of({}));
            mockServicesStore.connector.syncRoles = (structureId: string, connectorId: string) => {
                return new Promise<void>((res, err) => {});
            }
            
            component.onAddMassAssignment(profiles);
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            // TODO expect servicesStore.connector.syncRoles haveBeenCalled
            expect(mockServicesService.massAssignConnector).toHaveBeenCalled();
            expect(mockNotifyService.success).toHaveBeenCalled();
        });
    });

    describe('onRemoveMassAssignment', () => {
        it('should remove mass assignment for given profiles', () => {
            const profiles: Profile[] = ['Personnel'];
            (mockServicesService.massUnassignConnector as jasmine.Spy).and.returnValue(Observable.of({}));
            mockServicesStore.connector.syncRoles = (structureId: string, connectorId: string) => {
                return new Promise<void>((res, err) => {});
            }

            component.onRemoveMassAssignment(profiles);
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            expect(mockServicesService.massUnassignConnector).toHaveBeenCalled();
            // TODO expect servicesStore.connector.syncRoles haveBeenCalled            
            expect(mockNotifyService.success).toHaveBeenCalled();
        });
    });

    describe('onExportSubmit', () => {
        it('should call window.open with ServicesService.getExportConnectorUrl() with given exportFormat, profile and structureId', () => {
            const givenExportFormat: ExportFormat = {
                format: 'csv',
                value: 'Gepi',
                label: 'Gepi',
                profiles: ['Teacher', 'Student', 'Relative', 'Guest', 'Personnel']
            };
            const givenProfile: Profile = 'Teacher';
            const givenStructureId: string = 'structure1';
            const expectedUrl: string = `/directory/export/users?format=${givenExportFormat.format}&structureId=${givenStructureId}&type=${givenExportFormat.value}&profile=${givenProfile}`;
            (mockServicesService.getExportConnectorUrl as jasmine.Spy).and.returnValue(expectedUrl);
            spyOn(window, 'open');

            component.onExportSubmit({
                exportFormat: givenExportFormat,
                profile: givenProfile
            });

            expect(mockServicesService.getExportConnectorUrl).toHaveBeenCalled();
            expect(window.open).toHaveBeenCalledWith(expectedUrl, '_blank');
        })
    });

    describe('onIconFileChanged', () => {
        it('should call ServicesService.uploadPublicImage', () => {
            let file: File = {} as File;
            const resId: string = 'idDoc1';
            
            (mockServicesService.uploadPublicImage as jasmine.Spy).and.returnValue(Observable.of({_id: resId}));
            
            component.connectorPropertiesComponent.propertiesFormRef = <NgForm>{};
            component.connectorPropertiesComponent.propertiesFormRef.form = new FormBuilder().group({});
            spyOn(component.connectorPropertiesComponent.propertiesFormRef.form, 'markAsDirty');

            component.onIconFileChanged([file]);
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            expect(mockServicesService.uploadPublicImage).toHaveBeenCalledWith(file);
            expect(mockServicesStore.connector.icon).toBe(`/workspace/document/${resId}`);
            expect(component.connectorPropertiesComponent.propertiesFormRef.form.markAsDirty).toHaveBeenCalled();
        });
    })
})

@Component({
    selector: 'connector-properties',
    template: ''
})
class MockConnectorPropertiesComponent {
    @Input()
    connector: ConnectorModel = new ConnectorModel();
    @Input()
    casTypes: CasType[] = [];
    @Input()
    structureChildren: boolean = false;
    @Input()
    creationMode: boolean = false;
    @Input()
    disabled: boolean = false;

    @Output()
    create: EventEmitter<string> = new EventEmitter<string>();
    @Output()
    iconFileChanged: EventEmitter<File[]> = new EventEmitter();
    @Output()
    iconFileInvalid: EventEmitter<string> = new EventEmitter();
}

@Component({
    selector: 'connector-assignment',
    template: ''
})
class MockConnectorAssignmentComponent {
    @Input()
    connector: ConnectorModel = new ConnectorModel();
    @Input()
    disabled: boolean = false;

    @Output()
    remove: EventEmitter<Assignment> = new EventEmitter();
    @Output()
    add: EventEmitter<Assignment> = new EventEmitter(); 
}

@Component({
    selector: 'connector-mass-assignment',
    template: ''
})
class MockConnectorMassAssignmentComponent {
    @Input()
    structure: Structure;
    @Input()
    profiles: Array<Profile>;

    @Output()
    submitAssignment: EventEmitter<Array<Profile>> = new EventEmitter();
    @Output()
    submitUnassignment: EventEmitter<Array<Profile>> = new EventEmitter();
}

@Component({
    selector: 'connector-export',
    template: ''
})
class MockConnectorExport {
    @Output()
    submit: EventEmitter<{exportFormat: ExportFormat, profile: string}> = new EventEmitter<{exportFormat: ExportFormat, profile: string}>();
}