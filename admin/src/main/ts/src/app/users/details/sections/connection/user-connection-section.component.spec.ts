import {ChangeDetectorRef} from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {FormsModule} from '@angular/forms';
import {SijilModule} from 'sijil';
import {UxModule} from '../../../../shared/ux/ux.module';
import {UserConnectionSectionComponent} from './user-connection-section.component';
import {NotifyService, SpinnerService} from '../../../../core/services';
import {UserConnectionService} from './user-connection.service';

describe('UserInfoSectionComponent', () => {
    let component: UserConnectionSectionComponent;
    let fixture: ComponentFixture<UserConnectionSectionComponent>;

    let mockChangeDetectorRef: ChangeDetectorRef;
    let mockNotifyService: NotifyService;
    let mockSpinnerService: SpinnerService;
    let mockUserInfoService: UserConnectionService;
    let httpController: HttpTestingController;

    beforeEach(() => {
        mockChangeDetectorRef = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);
        mockNotifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        mockSpinnerService = jasmine.createSpyObj('SpinnerService', ['perform']);
        mockUserInfoService = jasmine.createSpyObj('UserInfoService', ['getState']);
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [
              UserConnectionSectionComponent
            ],
            providers: [
                {provide: NotifyService, useValue: mockNotifyService},
                {provide: SpinnerService, useValue: mockSpinnerService},
                {provide: ChangeDetectorRef, useValue: mockChangeDetectorRef},
                {provide: UserConnectionService, useValue: mockUserInfoService}
            ],
            imports: [
                HttpClientTestingModule,
                SijilModule.forRoot(),
                UxModule.forRoot(null),
                FormsModule
            ]

        }).compileComponents();
        fixture = TestBed.createComponent(UserConnectionSectionComponent);
        component = fixture.debugElement.componentInstance;
        httpController = TestBed.inject(HttpTestingController);
    }));

    it('should create the UserInfoSectionComponent component', waitForAsync(() => {
        expect(component).toBeTruthy();
    }));

    describe('generateRenewalCode', () => {
        it('should call the backend /auth/generatePasswordRenewalCode with given user login', () => {
            component.generateRenewalCode('myUserLogin').subscribe();
            const requestController = httpController.expectOne('/auth/generatePasswordRenewalCode');
            expect(requestController.request.method).toBe('POST');
            expect(requestController.request.body).toEqual('login=myUserLogin');
        });
    });
});
