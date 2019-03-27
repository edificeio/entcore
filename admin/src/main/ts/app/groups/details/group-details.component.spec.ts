import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { SijilModule } from 'sijil';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { UxModule } from '../../shared/ux/ux.module';
import { GroupsStore } from '../groups.store';
import { GroupDetails } from './group-details.component';
import { GroupModel, StructureModel } from '../../core/store/models';
import { GroupCollection } from '../../core/store/collections';
import { GroupIdAndInternalCommunicationRule } from './group-internal-communication-rule.resolver';
import { CommunicationRulesService } from '../../users/communication/communication-rules.service';
import { NotifyService } from '../../core/services';
import 'rxjs/add/observable/of';

describe('GroupDetails', () => {
    let component: GroupDetails;
    let fixture: ComponentFixture<GroupDetails>;

    let mockGroupStore: GroupsStore;
    let mockStructure: StructureModel;
    let mockCommunicationRulesService: CommunicationRulesService;
    let mockNotifyService: NotifyService;
    let mockChangeDetectorRef: ChangeDetectorRef;
    let paramsController: BehaviorSubject<{ [key: string]: string }>;
    let dataController: BehaviorSubject<{ rule: GroupIdAndInternalCommunicationRule }>;

    beforeEach(() => {
        mockStructure = {} as StructureModel;
        mockCommunicationRulesService = jasmine.createSpyObj('CommunicationRulesService', ['toggleInternalCommunicationRule']);
        mockNotifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
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
                {provide: CommunicationRulesService, useValue: mockCommunicationRulesService},
                {provide: NotifyService, useValue: mockNotifyService},
                {
                    provide: ActivatedRoute, useValue: {
                        data: dataController.asObservable(),
                        params: paramsController.asObservable()
                    }
                }
            ],
            imports: [
                SijilModule.forRoot(),
                UxModule.forRoot(null)
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(GroupDetails);
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

    it(`should call the communicationRulesService.toggleInternalCommunicationRule when clicking on the communication rules switch`, () => {
        fixture.nativeElement.querySelector('.lct-communication-rule').click();
        expect(mockCommunicationRulesService.toggleInternalCommunicationRule).toHaveBeenCalled();
    });
});

@Component({
    selector: 'group-manage-users',
    template: ''
})
class MockGroupManageUsers {
    @Output() close: EventEmitter<void> = new EventEmitter<void>();
}

@Component({
    selector: 'group-users-list',
    template: '<ng-content></ng-content>'
})
class MockGroupUsersList {
    @Input() users;
}

function generateMockGroupModel(id: string, type: string): GroupModel {
    const groupModel: GroupModel = {id, type} as GroupModel;
    groupModel.users = [];
    return groupModel;
}
