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
        
        TestBed.configureTestingModule({
            providers: [
                GroupsService,
                {provide: GroupsStore, useValue: mockGroupStore},
            ],
            imports: [HttpClientTestingModule]
        });
        groupsService = TestBed.get(GroupsService);
        httpTestingController = TestBed.get(HttpTestingController);
    })

    describe('delete', () => {
        it('should call DELETE /directory/group/groupId1 when given group with id "groupId1" and remove "groupId1" from groupsStore', () => {
            groupsService.groupsStore = mockGroupStore;
            const groupToDelete: GroupModel = new GroupModel();
            groupToDelete.id = 'groupId1';
            groupsService.delete(groupToDelete).subscribe(() => {
                const request = httpTestingController.expectOne(`/directory/group/groupId1`);
                expect(request.request.method).toBe('DELETE');
                console.log(mockGroupStore.structure.groups.data);
                expect(mockGroupStore.structure.groups.data.length).toBe(1);
            });
        })
    })
});
