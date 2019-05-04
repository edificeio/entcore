import { ConnectorCreate } from './connector-create.component';
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SijilModule } from 'sijil';
import { UxModule } from '../../../shared/ux/ux.module';
import { ServicesStore } from '../../services.store';
import { NotifyService, SpinnerService } from '../../../core/services';
import { StructureModel, ConnectorModel } from '../../../core/store';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { ServicesService } from '../../services.service';
import { Observable } from 'rxjs';

describe('ConnectorCreate', () => {
    let component: ConnectorCreate;
    let fixture: ComponentFixture<ConnectorCreate>;

    let mockServicesService: ServicesService;
    let mockServicesStore: ServicesStore;
    let mockNotifyService: NotifyService;
    let mockSpinnerService: SpinnerService;
    let mockRouter: Router;
    let mockLocation: Location;
    let connectorsDataPushSpy: jasmine.Spy;

    beforeEach(() => {
        mockServicesService = jasmine.createSpyObj('ServicesService', ['createConnector']);
        mockServicesStore = jasmine.createSpyObj('ServicesStore', ['onchange']);
        mockServicesStore.structure = new StructureModel();
        connectorsDataPushSpy = spyOn(mockServicesStore.structure.connectors.data, 'push');
        
        mockNotifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        mockSpinnerService = jasmine.createSpyObj('SpinnerService', ['perform']);
        mockRouter = jasmine.createSpyObj('Router', ['navigate']);
        mockLocation = jasmine.createSpyObj('Location', ['back']);
    });

    beforeEach(async(() => {
       TestBed.configureTestingModule({
           declarations: [ConnectorCreate],
           providers: [
               {provide: ServicesService, useValue: mockServicesService},
               {provide: ServicesStore, useValue: mockServicesStore},
               {provide: NotifyService, useValue: mockNotifyService},
               {provide: SpinnerService, useValue: mockSpinnerService},
               {provide: Router, useValue: mockRouter},
               {provide: ActivatedRoute, useValue: {params: convertToParamMap({})}},
               {provide: Location, useValue: mockLocation}
           ],
           imports: [
               SijilModule.forRoot(),
               FormsModule,
               UxModule.forRoot(null)
            ]
       }).compileComponents();

       fixture = TestBed.createComponent(ConnectorCreate);
       component = fixture.debugElement.componentInstance;
    }));

    it('should create the ConnectorCreate component', async(() => {
        expect(component).toBeTruthy();
    }));

    describe('create', () => {
        it('should create a new connector', () => {
            const newConnector: ConnectorModel = new ConnectorModel();
            newConnector.name = 'newConnector';

            component.newConnector= newConnector;
            mockServicesStore.structure.id = 'structureId';
            (mockServicesService.createConnector as jasmine.Spy).and.returnValue(Observable.of({id: 'connectorId', roleId: 'roleId'}));

            component.create();
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

    describe('cancel', () => {
        it('should call navigation.back', () => {
            component.cancel();
            expect(mockLocation.back).toHaveBeenCalled();
        });
    });
})