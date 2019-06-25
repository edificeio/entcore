import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { UserOverview } from './user-overview.component';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/map';

export interface BackendDirectoryUserResponse {
    zipCode: string;
    country: string;
    lastName: string;
    city: string;
    birthDate: string;
    displayName: string;
    source: string;
    login: string;
    title: string;
    surname: string;
    modified: string;
    id: string;
    email: string;
    address: string;
    created: string;
    mobile: string;
    profiles: string[];
    externalId: string;
    joinKey: string[];
    firstName: string;
    mobilePhone: string[];
    workPhone: string;
    activationCode: string;
    structureNodes: { source: string, type: string, name: string }[],
    type: string[];
}

@Injectable()
export class UserService {

    constructor(private http: HttpClient) {
    }

    public fetch(userId: string): Observable<UserOverview> {
        return this.http.get<BackendDirectoryUserResponse>(`/directory/user/${userId}?manual-groups=true`)
            .map((res: BackendDirectoryUserResponse): UserOverview => ({
                activationCode: res.activationCode,
                birthDate: res.birthDate,
                displayName: res.displayName,
                email: res.email,
                firstName: res.firstName,
                lastName: res.lastName,
                login: res.login,
                source: res.source,
                type: res.profiles[0],
                structures: res.structureNodes.map(node => node.name)
            }));
    }
}
