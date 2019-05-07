import { SmartConnectorComponent } from './smart-connector.component';
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SijilModule, BundlesService } from 'sijil';
import { UxModule } from '../../../shared/ux/ux.module';
import { ServicesStore } from '../../services.store';
import { NotifyService, SpinnerService } from '../../../core/services';
import { StructureModel, ConnectorModel, RoleModel, GroupModel, ConnectorCollection } from '../../../core/store';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { ServicesService } from '../../services.service';
import { Observable } from 'rxjs';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { By } from '@angular/platform-browser';
import { CasType } from './CasType';

describe('SmartConnector', () => {
    let component: SmartConnectorComponent;
    let fixture: ComponentFixture<SmartConnectorComponent>;

    let mockConnectorPropertiesComponent: MockConnectorPropertiesComponent;
    let mockConnectorAssignmentComponent: MockConnectorAssignmentComponent;
    let mockServicesService: ServicesService;
    let mockServicesStore: ServicesStore;
    let mockNotifyService: NotifyService;
    let mockSpinnerService: SpinnerService;
    let mockRouter: Router;
    let mockLocation: Location;
    let mockBundle: BundlesService;
    let connectorsDataPushSpy: jasmine.Spy;

    beforeEach(() => {
        mockServicesService = jasmine.createSpyObj('ServicesService', ['createConnector', 'saveConnector', 'getCasTypes', 'addGroupToRole', 'removeGroupFromRole']);
        mockServicesStore = jasmine.createSpyObj('ServicesStore', ['onchange']);
        mockServicesStore.structure = new StructureModel();
        mockServicesStore.structure.connectors = new ConnectorCollection();
        mockServicesStore.connector = new ConnectorModel();
        connectorsDataPushSpy = spyOn(mockServicesStore.structure.connectors.data, 'push');

        mockNotifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        mockSpinnerService = jasmine.createSpyObj('SpinnerService', ['perform']);
        mockRouter = jasmine.createSpyObj('Router', ['navigate']);
        mockLocation = jasmine.createSpyObj('Location', ['back']);
        mockBundle = jasmine.createSpyObj('BundlesService', ['translate'])
    });

    beforeEach(async(() => {
       TestBed.configureTestingModule({
            declarations: [
               SmartConnectorComponent, 
               MockConnectorPropertiesComponent, 
               MockConnectorAssignmentComponent
            ],
            providers: [
               {provide: ServicesService, useValue: mockServicesService},
               {provide: ServicesStore, useValue: mockServicesStore},
               {provide: NotifyService, useValue: mockNotifyService},
               {provide: SpinnerService, useValue: mockSpinnerService},
               {provide: Router, useValue: mockRouter},
               {provide: ActivatedRoute, useValue: {params: convertToParamMap({})}},
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
       fixture.detectChanges();
       mockConnectorPropertiesComponent = fixture.debugElement.query(By.directive(MockConnectorPropertiesComponent)).componentInstance;
       mockConnectorAssignmentComponent = fixture.debugElement.query(By.directive(MockConnectorAssignmentComponent)).componentInstance;
    }));

    it('should create the SmartConnectorComponent component', async(() => {
        expect(component).toBeTruthy();
    }));

    describe('create', () => {
        it('should create a new connector', () => {
            const newConnector: ConnectorModel = new ConnectorModel();
            newConnector.name = 'newConnector';

            mockServicesStore.structure.id = 'structureId';
            (mockServicesService.createConnector as jasmine.Spy).and.returnValue(Observable.of({id: 'connectorId', roleId: 'roleId'}));

            component.onCreate('submit');
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            expect(mockServicesService.createConnector).toHaveBeenCalled();

            const pushedConnector: ConnectorModel = connectorsDataPushSpy.calls.mostRecent().args[0];
            expect(pushedConnector.name).toBe('newConnector');
            expect(pushedConnector.id).toBe('connectorId');

            expect(mockRouter.navigate).toHaveBeenCalledWith(['..', pushedConnector.id]
                        , {relativeTo: {params: convertToParamMap({})}, replaceUrl: false});
            expect(mockNotifyService.success).toHaveBeenCalled();
        })
    });

    describe('onCreate with cancel event', () => {
        it('should call navigation.back', () => {
            component.onCreate('cancel');
            expect(mockLocation.back).toHaveBeenCalled();
        });
    });
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
    isAdmc: boolean = false;
    @Input()
    hasChildren: boolean = false;
    @Input()
    isCreationMode: boolean = false;

    @Output()
    create: EventEmitter<string> = new EventEmitter<string>();
    @Output()
    save: EventEmitter<string> = new EventEmitter<string>();
}

@Component({
    selector: 'connector-assignment',
    template: ''
})
class MockConnectorAssignmentComponent {
    @Input()
    connector: ConnectorModel = new ConnectorModel();

    @Output()
    remove: EventEmitter<{group: GroupModel, role: RoleModel}> = new EventEmitter();
    @Output()
    add: EventEmitter<{group: GroupModel, role: RoleModel}> = new EventEmitter();
}