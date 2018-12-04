import { ChangeDetectorRef } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { SijilModule } from 'sijil';
import { UxModule } from '../../../../shared/ux/ux.module';
import { NotifyService, SpinnerService } from '../../../../core/services';
import { GlobalStore } from '../../../../core/store';
import { UserManualGroupsSection } from './user-manualgroups-section.component';

describe('UserManualGroupsSection', () => {
    let component: UserManualGroupsSection;
    let fixture: ComponentFixture<UserManualGroupsSection>;

    let mockChangeDetectorRef: ChangeDetectorRef;
    let mockNotifyService: NotifyService;
    let mockSpinnerService: SpinnerService;
    let globalStore: GlobalStore;

    beforeEach(() => {
        mockChangeDetectorRef = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);
        mockNotifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
        mockSpinnerService = jasmine.createSpyObj('SpinnerService', ['perform']);
        globalStore = new GlobalStore();
    });

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                UserManualGroupsSection
            ],
            providers: [
                {provide: NotifyService, useValue: mockNotifyService},
                {provide: SpinnerService, useValue: mockSpinnerService},
                {provide: ChangeDetectorRef, useValue: mockChangeDetectorRef},
                {provide: GlobalStore, useValue: globalStore}
            ],
            imports: [
                SijilModule.forRoot(),
                UxModule.forRoot(null),
                FormsModule
            ]

        }).compileComponents();
        fixture = TestBed.createComponent(UserManualGroupsSection);
        component = fixture.debugElement.componentInstance;
    }));

    it('should create the UserManualGroupsSection component', async(() => {
        expect(component).toBeTruthy();
    }));
});
