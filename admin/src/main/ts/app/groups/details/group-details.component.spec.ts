import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { SijilModule } from 'sijil';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import 'rxjs/add/observable/of';

import { UxModule } from '../../shared/ux/ux.module';
import { GroupsStore } from '../groups.store';
import { GroupDetails } from './group-details.component';
import { GroupModel, StructureModel } from '../../core/store/models';
import { GroupCollection } from '../../core/store/collections';
import { GroupIdAndInternalCommunicationRule } from './group-internal-communication-rule.resolver';

@Component({
    selector: 'group-manage-users',
    template: ''
})
class MockGroupManageUsers {
    @Output() close: EventEmitter<void> = new EventEmitter<void>();
}

@Component({
    selector: 'group-users-list',
    template: ''
})
class MockGroupUsersList {
    @Input() users;
}

function generateMockGroupModel(id: string, type: string): GroupModel {
    const groupModel: GroupModel = {id, type} as GroupModel;
    groupModel.users = [];
    return groupModel;
}

describe('GroupDetails', () => {
    let component: GroupDetails;
    let fixture: ComponentFixture<GroupDetails>;

    let mockGroupStore: GroupsStore;
    let mockStructure: StructureModel;
    let mockChangeDetectorRef: ChangeDetectorRef;
    let httpController: HttpTestingController;
    let paramsController: BehaviorSubject<{ [key: string]: string }>;
    let dataController: BehaviorSubject<{ rule: GroupIdAndInternalCommunicationRule }>;

    beforeEach(() => {
        mockStructure = {} as StructureModel;
        mockStructure.groups = {
            data: [
                generateMockGroupModel('groupId1', 'ManualGroup'),
                generateMockGroupModel('groupId2', 'OtherGroup')
            ]
        } as GroupCollection;
        mockGroupStore = {structure: mockStructure} as GroupsStore;
        mockChangeDetectorRef = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);
        paramsController = new BehaviorSubject({groupId: 'groupId1'});
        dataController = new BehaviorSubject<{ rule: GroupIdAndInternalCommunicationRule }>({
            rule: {
                groupId: 'groupId1',
                internalCommunicationRule: 'BOTH'
            }
        });
    });

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                MockGroupManageUsers,
                MockGroupUsersList,
                GroupDetails
            ],
            providers: [
                {provide: GroupsStore, useValue: mockGroupStore},
                {provide: ChangeDetectorRef, useValue: mockChangeDetectorRef},
                {
                    provide: ActivatedRoute, useValue: {
                        data: dataController.asObservable(),
                        params: paramsController.asObservable()
                    }
                }
            ],
            imports: [
                HttpClientTestingModule,
                SijilModule.forRoot(),
                UxModule.forRoot(null)
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(GroupDetails);
        httpController = TestBed.get(HttpTestingController);
        component = fixture.debugElement.componentInstance;
        fixture.detectChanges();
    }));

    it('should create the GroupDetails component', async(() => {
        expect(component).toBeTruthy();
    }));

    it('should set the current group based on the groupId route params', () => {
        expect(component.groupsStore.group).toEqual(generateMockGroupModel('groupId1', 'ManualGroup'));
    });

    it('should display the internal communication rule when the current group type is ManualGroup', () => {
        expect(fixture.nativeElement.querySelectorAll('.lct-communication-rule').length).toBe(1);
    });

    it('should not display the internal communication rule when the current group type is not ManualGroup', () => {
        paramsController.next({groupId: 'groupId2'});
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelectorAll('.lct-communication-rule').length).toBe(0);
    });

    it(`should display the 'can-communicate' element when the group type is ManualGroup and internal communication rule is 'BOTH'`, () => {
        expect(fixture.nativeElement.querySelectorAll('.lct-communication-rule__can-communicate').length).toBe(1);
        expect(fixture.nativeElement.querySelectorAll('.lct-communication-rule__cannot-communicate').length).toBe(0);
    });

    it(`should display the 'cannot-communicate' element when the group type is ManualGroup and internal communication rule is 'INCOMING'`, () => {
        dataController.next({rule: {groupId: 'groupId1', internalCommunicationRule: 'INCOMING'}});
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelectorAll('.lct-communication-rule__cannot-communicate').length).toBe(1);
        expect(fixture.nativeElement.querySelectorAll('.lct-communication-rule__can-communicate').length).toBe(0);
    });

    describe('toggleCommunicationBetweenMembers', () => {
        it(`should call the backend DELETE /communication/group/myGroupId with direction BOTH given group id 'myGroupId' and internal communication rule 'BOTH'`, () => {
            component.toggleCommunicationBetweenMembers('myGroupId', 'BOTH').subscribe();
            const communicationGroupRequest = httpController.expectOne('/communication/group/myGroupId');
            expect(communicationGroupRequest.request.method).toBe('DELETE');
            expect(communicationGroupRequest.request.body).toEqual({
                direction: 'BOTH'
            });
        });

        it(`should call the backend /communication/group/myGroupId with direction BOTH given group id 'myGroupId' and internal communication rule 'NONE'`, () => {
            component.toggleCommunicationBetweenMembers('myGroupId', 'NONE').subscribe();
            expect(httpController.expectOne('/communication/group/myGroupId').request.body).toEqual({
                direction: 'BOTH'
            });
        });

        it(`should call the backend /communication/group/myGroupId with direction BOTH given group id 'myGroupId' and internal communication rule 'INCOMING'`, () => {
            component.toggleCommunicationBetweenMembers('myGroupId', 'INCOMING').subscribe();
            expect(httpController.expectOne('/communication/group/myGroupId').request.body).toEqual({
                direction: 'BOTH'
            });
        });

        it(`should call the backend /communication/group/myGroupId with direction BOTH given group id 'myGroupId' and internal communication rule 'OUTGOING'`, () => {
            component.toggleCommunicationBetweenMembers('myGroupId', 'OUTGOING').subscribe();
            expect(httpController.expectOne('/communication/group/myGroupId').request.body).toEqual({
                direction: 'BOTH'
            });
        });
    });

    it(`should set the internalCommunicationRule of the group to 'NONE'
        when clicking on the communication rule toggle button and given the current group has the 'BOTH' rule`, () => {
        fixture.nativeElement.querySelector('.lct-communication-rule').click();
        httpController.expectOne('/communication/group/groupId1').flush({number: 1});
        expect(component.internalCommunicationRule).toBe('NONE');
    });

    it(`should set the internalCommunicationRule of the group to 'BOTH'
        when clicking on the communication rule toggle button and given the current group has the 'NONE' rule`, () => {
        dataController.next({rule: {groupId: 'groupId1', internalCommunicationRule: 'NONE'}});
        fixture.detectChanges();
        fixture.nativeElement.querySelector('.lct-communication-rule').click();
        httpController.expectOne('/communication/group/groupId1').flush({number: 1});
        expect(component.internalCommunicationRule).toBe('BOTH');
    });

    it(`should set the internalCommunicationRule of the group to 'BOTH'
        when clicking on the communication rule toggle button and given the current group has the 'INCOMING' rule`, () => {
        dataController.next({rule: {groupId: 'groupId1', internalCommunicationRule: 'INCOMING'}});
        fixture.detectChanges();
        fixture.nativeElement.querySelector('.lct-communication-rule').click();
        httpController.expectOne('/communication/group/groupId1').flush({number: 1});
        expect(component.internalCommunicationRule).toBe('BOTH');
    });

    it(`should set the internalCommunicationRule of the group to 'BOTH'
        when clicking on the communication rule toggle button and given the current group has the 'OUTGOING' rule`, () => {
        dataController.next({rule: {groupId: 'groupId1', internalCommunicationRule: 'OUTGOING'}});
        fixture.detectChanges();
        fixture.nativeElement.querySelector('.lct-communication-rule').click();
        httpController.expectOne('/communication/group/groupId1').flush({number: 1});
        expect(component.internalCommunicationRule).toBe('BOTH');
    });
});
