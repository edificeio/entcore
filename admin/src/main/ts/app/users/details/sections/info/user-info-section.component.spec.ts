import { ChangeDetectorRef } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { SijilModule } from 'sijil';
import { UxModule } from '../../../../shared/ux/ux.module';
import { UserInfoSection } from './user-info-section.component';
import { NotifyService, SpinnerService } from '../../../../core/services';
import { UserInfoService } from './user-info.service';

describe('UserInfoSection', () => {
    let component: UserInfoSection;
    let fixture: ComponentFixture<UserInfoSection>;

    let mockChangeDetectorRef: ChangeDetectorRef;
    let mockNotifyService: NotifyService;
    let mockSpinnerService: SpinnerService;
    let mockUserInfoService: UserInfoService;
    let httpController: HttpTestingController;

    beforeEach(() => {
        mockChangeDetectorRef = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);
        mockNotifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        mockSpinnerService = jasmine.createSpyObj('SpinnerService', ['perform']);
        mockUserInfoService = jasmine.createSpyObj('UserInfoService', ['getState']);
    });

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                UserInfoSection
            ],
            providers: [
                {provide: NotifyService, useValue: mockNotifyService},
                {provide: SpinnerService, useValue: mockSpinnerService},
                {provide: ChangeDetectorRef, useValue: mockChangeDetectorRef},
                {provide: UserInfoService, useValue: mockUserInfoService}
            ],
            imports: [
                HttpClientTestingModule,
                SijilModule.forRoot(),
                UxModule.forRoot(null),
                FormsModule
            ]

        }).compileComponents();
        fixture = TestBed.createComponent(UserInfoSection);
        component = fixture.debugElement.componentInstance;
        httpController = TestBed.get(HttpTestingController);
    }));

    it('should create the UserInfoSection component', async(() => {
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
