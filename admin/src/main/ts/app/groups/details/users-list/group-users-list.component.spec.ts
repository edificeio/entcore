import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { SijilModule } from 'sijil';
import { GroupUsersList } from './group-users-list.component';
import { UserListService } from '../../../core/services';
import { UxModule } from '../../../shared/ux/ux.module';
import { UserModel } from '../../../core/store/models';

describe('GroupUsersList', () => {
    let component: GroupUsersList;
    let fixture: ComponentFixture<GroupUsersList>;

    let mockRouter: Router;
    let mockUserListService: UserListService;

    beforeEach(() => {
        mockRouter = jasmine.createSpyObj('Router', ['navigate']);
        mockUserListService = jasmine.createSpyObj('UserListService', ['changeSorts', 'filterByInput']);
        mockUserListService.sorts = ['+lastName', '+firstName', '+type'];
        mockUserListService.sortsMap = {
            alphabetical: {
                sort: '+',
                orderedValue: 'lastName',
                staticValues: ['+firstName'],
                selected: true
            },
            profile: {
                sort: '+',
                orderedValue: 'type',
                selected: false
            }
        };
    });

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                GroupUsersList
            ],
            providers: [
                {provide: UserListService, useValue: mockUserListService},
                {provide: Router, useValue: mockRouter}
            ],
            imports: [
                SijilModule.forRoot(),
                UxModule.forRoot(null)
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(GroupUsersList);
        component = fixture.debugElement.componentInstance;
    }));

    it('should create the GroupUsersList component', async(() => {
        expect(component).toBeTruthy();
    }));

    describe('selectUser', () => {
        it('should navigate to /admin/firstStructureId/users/userId', () => {
            const selectedUser = {
                id: 'userId',
                structures: [
                    {id: 'firstStructureId', name: 'first structure'},
                    {id: 'secondStructureId', name: 'second structure'}
                ]
            };
            component.selectUser(selectedUser as UserModel);
            expect(mockRouter.navigate).toHaveBeenCalledWith(['admin', 'firstStructureId', 'users', 'userId']);
        });
    });
});