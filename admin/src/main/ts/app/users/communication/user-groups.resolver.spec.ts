import { UserGroupsResolver } from './user-groups.resolver';
import { ActivatedRouteSnapshot, convertToParamMap } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { generateGroup } from "./communication-test-utils";
import { GroupModel } from "../../core/store/models";

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
        userGroupsResolver.resolve(generateActivatedRouteSnapshot('myUser'))
            .subscribe();
        httpController.expectOne('/directory/user/myUser/groups');
    });

    it('should filter user groups to remove CommunityGroup', () => {
        let groups: GroupModel[] = [];

        userGroupsResolver.resolve(generateActivatedRouteSnapshot('myUser'))
            .subscribe(g => groups = g);
        httpController.expectOne('/directory/user/myUser/groups').flush([
            generateGroup('manualGroup', 'BOTH', 'ManualGroup'),
            generateGroup('communityGroup', 'BOTH', 'CommunityGroup'),
            generateGroup('functionGroup', 'BOTH', 'FunctionGroup')
        ]);
        expect(groups.map(g => g.id)).toEqual(['manualGroup', 'functionGroup']);
    });
});

function generateActivatedRouteSnapshot(userId?: string): ActivatedRouteSnapshot {
    return {paramMap: convertToParamMap({userId})} as ActivatedRouteSnapshot;
}
