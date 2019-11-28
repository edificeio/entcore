import {TestBed} from '@angular/core/testing';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {UsersService} from './users.service';

describe('UserService', () => {
    let userService: UsersService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                UsersService,
            ],
            imports: [HttpClientTestingModule]
        });
        userService = TestBed.get(UsersService);
        httpTestingController = TestBed.get(HttpTestingController);
    });

    describe('fetch', () => {
        it('should call GET "/directory/user/userA?manual-groups=true" when given user id "userA"', () => {
            userService.fetch('userA').subscribe();
            const request = httpTestingController.expectOne(`/directory/user/userA?manual-groups=true`);
            expect(request.request.method).toBe('GET');
        });
    });
});
