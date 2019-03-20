import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import { HttpClient } from '@angular/common/http';
import { InternalCommunicationRule } from '../../core/store/models';

export interface CommunicationGroupResponse {
    groupDisplayName: string;
    displayNameSearchField: string;
    name: string;
    id: string;
    users: InternalCommunicationRule | undefined;
    communiqueWith: Array<string>;
}

export interface GroupIdAndInternalCommunicationRule {
    groupId: string;
    internalCommunicationRule: InternalCommunicationRule;
}

@Injectable()
export class GroupInternalCommunicationRuleResolver implements Resolve<GroupIdAndInternalCommunicationRule> {
    constructor(private http: HttpClient) {
    }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<GroupIdAndInternalCommunicationRule> {
        if(!route.paramMap.has('groupId')) {
            return Observable.throw(new Error('no groupId'));
        }

        return this.http.get<CommunicationGroupResponse>(`/communication/group/${route.paramMap.get('groupId')}`)
            .map(response => ({
                groupId: response.id,
                internalCommunicationRule: response.users? response.users : 'NONE'
            }));
    }
}
