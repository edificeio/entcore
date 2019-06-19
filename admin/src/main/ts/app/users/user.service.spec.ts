import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UserService } from './user.service';

describe('UserService', () => {
    let userService: UserService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                UserService,
            ],
            imports: [HttpClientTestingModule]
        });
        userService = TestBed.get(UserService);
        httpTestingController = TestBed.get(HttpTestingController);
    });

    describe('fetch', () => {
        it('should call GET "/directory/user/userA?manual-groups=true" when given user id "userA"', () => {
            userService.fetch('userA').subscribe();
            const request = httpTestingController.expectOne(`/directory/user/userA?manual-groups=true`);
            expect(request.request.method).toBe('GET');
        })
    })
});
