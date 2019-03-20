import { UserGroupsResolver } from './user-groups.resolver';
import { ActivatedRouteSnapshot, convertToParamMap } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

describe('UserGroupsResolver', () => {
    let userGroupsResolver: UserGroupsResolver;
    let httpController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                UserGroupsResolver
            ],
            imports: [
                HttpClientTestingModule
            ]
        });
        userGroupsResolver = TestBed.get(UserGroupsResolver);
        httpController = TestBed.get(HttpTestingController);
    });

    it('should call the backend API for listing groups of a user', () => {
        userGroupsResolver.resolve(generateActivatedRouteSnapshot('myUser')).subscribe();
        httpController.expectOne('/directory/user/myUser/groups');
    });
});

function generateActivatedRouteSnapshot(userId?: string): ActivatedRouteSnapshot {
    return {paramMap: convertToParamMap({userId})} as ActivatedRouteSnapshot;
}
