import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {UserOverview} from './user-overview/user-overview.component';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import { UserModel } from '../core/store/models/user.model';

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
    structureNodes: { source: string, type: string, name: string }[];
    type: string[];
}

@Injectable()
export class UsersService {

    constructor(private http: HttpClient) {
    }

    public fetch(userId: string): Observable<UserOverview> {
        return this.http.get<BackendDirectoryUserResponse>(`/directory/user/${userId}?manual-groups=true`)
        .pipe(
            map((res: BackendDirectoryUserResponse): UserOverview => ({
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
            }))
        );
    }

    public async search(searchTerm: string, searchType?: string, structureId?: string): Promise<UserModel[]> {
        if (!searchTerm) return [];

        // let term = searchTerm.replace(/[^-0-9a-zÀ-ÿ]/g, ''); // I had to remove that for email search
        
        let structureParam = structureId ? ("&structureId=" + structureId + "&includeSubStructures=true") : "";
        
        let res: Array<UserModel> = await this.
            http.
            get<Array<UserModel>>(`/directory/user/admin/list?searchTerm=${searchTerm}&searchType=${searchType}${structureParam}`).
            toPromise();

        return res;
    }
}
