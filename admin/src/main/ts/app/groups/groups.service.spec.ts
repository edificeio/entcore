import { GroupsService } from './groups.service';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { GroupModel, GroupCollection, StructureModel } from '../core/store';
import { GroupsStore } from './groups.store';
import { generateMockGroupModel } from '../shared/utils';

describe('GroupsService', () => {
    let groupsService: GroupsService;
    let httpTestingController: HttpTestingController;
    let mockGroupStore: GroupsStore;
    let mockStructure: StructureModel;

    beforeEach(() => {
        mockStructure = {} as StructureModel;
        mockStructure.groups = {
            data: [
                generateMockGroupModel('groupId1', 'ManualGroup'),
                generateMockGroupModel('groupId2', 'OtherGroup')
            ]
        } as GroupCollection;
        mockGroupStore = {structure: mockStructure} as GroupsStore;
        mockGroupStore.group = generateMockGroupModel('groupId1', 'ManualGroup');
        
        TestBed.configureTestingModule({
            providers: [
                GroupsService,
                {provide: GroupsStore, useValue: mockGroupStore},
            ],
            imports: [HttpClientTestingModule]
        });
        groupsService = TestBed.get(GroupsService);
        groupsService.groupsStore = mockGroupStore;
        httpTestingController = TestBed.get(HttpTestingController);
    });

    describe('delete', () => {
        it('should call DELETE /directory/group/groupId1 when given group with id "groupId1" and remove "groupId1" from groupsStore', () => {
            const groupToDelete: GroupModel = new GroupModel();
            groupToDelete.id = 'groupId1';
            groupsService.delete(groupToDelete).subscribe();
            const request = httpTestingController.expectOne(`/directory/group/groupId1`);
            request.flush({});
            expect(request.request.method).toBe('DELETE');
            expect(mockGroupStore.structure.groups.data.length).toBe(1);
        })
    })

    describe('update', () => {
        it('should call PUT /directory/group/groupId1 when given group with id "groupId1" and update group name with "group1" in groupsStore.group and groupsStore.structure.groups', () => {
            groupsService.update({id: 'groupId1', name: 'newName'}).subscribe();
            const request = httpTestingController.expectOne('/directory/group/groupId1');
            request.flush({});
            expect(request.request.method).toBe('PUT');
            expect(mockGroupStore.group.name).toBe('newName');
            expect(mockGroupStore.structure.groups.data.find(g => g.id === 'groupId1').name).toBe('newName');
        });
    })
});
