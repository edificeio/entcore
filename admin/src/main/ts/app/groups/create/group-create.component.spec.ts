import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { GroupCreate } from './group-create.component';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { Location } from '@angular/common';
import { UxModule } from '../../shared/ux/ux.module';
import { GroupsStore } from '../groups.store';
import { NotifyService, SpinnerService } from '../../core/services';
import { SijilModule } from 'sijil';

describe('GroupCreate', () => {
    let component: GroupCreate;
    let fixture: ComponentFixture<GroupCreate>;

    let mockGroupsStore: GroupsStore;
    let mockNotifyService: NotifyService;
    let mockSpinnerService: SpinnerService;
    let mockRouter: Router;
    let mockLocation: Location;

    beforeEach(() => {
        mockGroupsStore = jasmine.createSpyObj('GroupsStore', ['onchange']);
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
                SijilModule.forRoot(),
                UxModule.forRoot(null),
                FormsModule
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(GroupCreate);
        component = fixture.debugElement.componentInstance;
    }));

    it('should create the GroupCreate component', async(() => {
        expect(component).toBeTruthy();
    }));

    describe('cancel', () => {
        it('should call navigation.back', () => {
            component.cancel();
            expect(mockLocation.back).toHaveBeenCalled();
        });
    });
});
