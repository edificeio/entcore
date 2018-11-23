import { ActivatedRouteSnapshot, convertToParamMap, RouterStateSnapshot } from '@angular/router';
import { async, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import {
CommunicationGroupResponse,
GroupInternalCommunicationRuleResolver,
InternalCommunicationRule
} from './group-internal-communication-rule.resolver';

describe('GroupInternalCommunicationRuleResolver', () => {
    let service: GroupInternalCommunicationRuleResolver;
    let httpController: HttpTestingController;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            providers: [
                GroupInternalCommunicationRuleResolver
            ],
            imports: [
                HttpClientTestingModule
            ]
        }).compileComponents();
        service = TestBed.get(GroupInternalCommunicationRuleResolver);
        httpController = TestBed.get(HttpTestingController);
    }));

    it(`should call '/communication/group/myGroupId' when the current group id is 'myGroupId'`, () => {
        service.resolve(generateActivatedRouteSnapshot('myGroupId'), generateRouterStateSnapshot())
            .subscribe();

        const requestController = httpController.expectOne('/communication/group/myGroupId');
        expect(requestController.request.method).toBe('GET');
    });

    it(`should return 'BOTH' when the internal communication rule is 'BOTH' for the group 'myGroupId'`, () => {
        service.resolve(generateActivatedRouteSnapshot('myGroupId'), generateRouterStateSnapshot())
            .subscribe(res => expect(res.internalCommunicationRule).toBe('BOTH'), () => fail());

        httpController.expectOne('/communication/group/myGroupId')
            .flush(generateCommunicationGroupResponse('BOTH'));
    });
});


function generateActivatedRouteSnapshot(groupId?: string): ActivatedRouteSnapshot {
    return {paramMap: convertToParamMap({groupId})} as ActivatedRouteSnapshot;
}

function generateRouterStateSnapshot(): RouterStateSnapshot {
    return {} as RouterStateSnapshot;
}

function generateCommunicationGroupResponse(users: InternalCommunicationRule): CommunicationGroupResponse {
    return {
        users,
        communiqueWith: [],
        displayNameSearchField: '',
        groupDisplayName: '',
        id: 'myGroupId',
        name: 'myGroup'
    };
}
