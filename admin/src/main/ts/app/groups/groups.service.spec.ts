import { GroupsService } from './groups.service';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { GroupModel } from '../core/store';

describe('GroupsService', () => {
    let groupsService: GroupsService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [GroupsService],
            imports: [HttpClientTestingModule]
        });
        groupsService = TestBed.get(GroupsService);
        httpTestingController = TestBed.get(HttpTestingController);
    })

    describe('delete', () => {
        it('should call DELETE /directory/group/group1 when given group with id "group1"', () => {
            const groupToDelete: GroupModel = new GroupModel();
            groupToDelete.id = 'group1';
            groupsService.delete(groupToDelete).subscribe();
            const request = httpTestingController.expectOne(`/directory/group/group1`);
            expect(request.request.method).toBe('DELETE');
        })
    })
});
