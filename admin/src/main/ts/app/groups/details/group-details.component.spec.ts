import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { SijilModule } from 'sijil';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';

import { UxModule } from '../../shared/ux/ux.module';
import { GroupsStore } from '../groups.store';
import { GroupDetails } from './group-details.component';
import { GroupModel, StructureModel } from '../../core/store/models';
import { GroupCollection } from '../../core/store/collections';

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

    beforeEach(() => {
        mockStructure = {} as StructureModel;
        mockStructure.groups = {
            data: [
                generateMockGroupModel('groupId1', 'Manual'),
                generateMockGroupModel('groupId2', 'Other')
            ]
        } as GroupCollection;
        mockGroupStore = {structure: mockStructure} as GroupsStore;
        mockChangeDetectorRef = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);
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
                {provide: ActivatedRoute, useValue: {params: Observable.of({groupId: 'groupId1'})}}
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
        expect(component.groupsStore.group).toEqual(generateMockGroupModel('groupId1', 'Manual'));
    });
});