import { ChangeDetectorRef } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
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
                SijilModule.forRoot(),
                UxModule.forRoot(null),
                FormsModule
            ]

        }).compileComponents();
        fixture = TestBed.createComponent(UserInfoSection);
        component = fixture.debugElement.componentInstance;
    }));

    it('should create the UserInfoSection component', async(() => {
        expect(component).toBeTruthy();
    }));
});