import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { GroupCreate } from './group-create.component';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { Location } from '@angular/common';
import { UxModule } from '../../shared/ux/ux.module';
import { GroupsStore } from '../groups.store';
import { NotifyService, SpinnerService } from '../../core/services';
import { SijilModule } from 'sijil';
import { GroupModel, StructureModel } from '../../core/store/models';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

describe('GroupCreate', () => {
    let component: GroupCreate;
    let fixture: ComponentFixture<GroupCreate>;

    let mockGroupsStore: GroupsStore;
    let mockNotifyService: NotifyService;
    let mockSpinnerService: SpinnerService;
    let mockRouter: Router;
    let mockLocation: Location;
    let httpController: HttpTestingController;
    let groupsDataPushSpy: jasmine.Spy;

    beforeEach(() => {
        mockGroupsStore = jasmine.createSpyObj('GroupsStore', ['onchange']);
        mockGroupsStore.structure = new StructureModel();
        groupsDataPushSpy = spyOn(mockGroupsStore.structure.groups.data, 'push');
        mockNotifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        mockSpinnerService = jasmine.createSpyObj('SpinnerService', ['perform']);
        mockRouter = jasmine.createSpyObj('Router', ['navigate']);
        mockLocation = jasmine.createSpyObj('Location', ['back']);
    });

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                GroupCreate
            ],
            providers: [
                {provide: GroupsStore, useValue: mockGroupsStore},
                {provide: NotifyService, useValue: mockNotifyService},
                {provide: SpinnerService, useValue: mockSpinnerService},
                {provide: Router, useValue: mockRouter},
                {provide: ActivatedRoute, useValue: {params: convertToParamMap({})}},
                {provide: Location, useValue: mockLocation}
            ],
            imports: [
                HttpClientTestingModule,
                SijilModule.forRoot(),
                UxModule.forRoot(null),
                FormsModule
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(GroupCreate);
        component = fixture.debugElement.componentInstance;
        httpController = TestBed.get(HttpTestingController);
    }));

    it('should create the GroupCreate component', async(() => {
        expect(component).toBeTruthy();
    }));

    describe('createNewGroup', () => {
        it('should create a new group and set group internal rule to both way when the backend respond a group id', () => {
            component.newGroup.name = 'groupName';
            mockGroupsStore.structure.id = 'structureId';

            component.createNewGroup();
            expect(mockSpinnerService.perform).toHaveBeenCalled();
            const creationGroupRequest = httpController.expectOne('/directory/group');
            expect(creationGroupRequest.request.method).toBe('POST');
            expect(creationGroupRequest.request.body).toEqual({
                name: 'groupName',
                structureId: 'structureId'
            });
            creationGroupRequest.flush({id: 'groupId'});

            const communicationGroupRequest = httpController.expectOne('/communication/group/groupId');
            expect(communicationGroupRequest.request.method).toBe('POST');
            expect(communicationGroupRequest.request.body).toEqual({
                direction: 'BOTH'
            });
            communicationGroupRequest.flush({number: 1});

            const pushedGroupModel: GroupModel = groupsDataPushSpy.calls.mostRecent().args[0];
            expect(pushedGroupModel.name).toBe('groupName');
            expect(pushedGroupModel.structureId).toBe('structureId');
            expect(pushedGroupModel.id).toBe('groupId');

            expect(mockNotifyService.success).toHaveBeenCalled();
        });

        it('should display an error message when the backend respond an error to group creation request', () => {
            component.newGroup.name = 'groupName';
            mockGroupsStore.structure.id = 'structureId';

            component.createNewGroup();
            httpController.expectOne('/directory/group')
                .flush({}, {status: 500, statusText: 'Internal server error'});
            expect(groupsDataPushSpy).not.toHaveBeenCalled();
            expect(mockNotifyService.error).toHaveBeenCalled();
        });

        it('should display an error message when the backend respond an error to group communication request', () => {
            component.newGroup.name = 'groupName';
            mockGroupsStore.structure.id = 'structureId';

            component.createNewGroup();
            httpController.expectOne('/directory/group').flush({id: 'groupId'});
            httpController.expectOne('/communication/group/groupId').flush({}, {
                status: 500,
                statusText: 'Internal server error'
            });

            expect(groupsDataPushSpy).not.toHaveBeenCalled();
            expect(mockNotifyService.error).toHaveBeenCalled();
        });
    });

    describe('cancel', () => {
        it('should call navigation.back', () => {
            component.cancel();
            expect(mockLocation.back).toHaveBeenCalled();
        });
    });
});
