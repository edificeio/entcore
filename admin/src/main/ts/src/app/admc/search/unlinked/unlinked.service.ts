import {Injectable} from '@angular/core';
import http from 'axios';
import { Profile } from 'src/app/services/_shared/services-types';
import { NotifyService } from '../../../core/services/notify.service';
import { BackendDirectoryUserResponse } from 'src/app/users/users.service';
import { SearchTypeEnum } from "src/app/core/enum/SearchTypeEnum";


export type UnlinkedUser = {
    id: string;
    displayName: string;
    firstName: string;
    lastName: string;
    type?: Profile;
    code?: string;
}

export type UnlinkedUserDetails = BackendDirectoryUserResponse & {
    blocked?: boolean;
    functions?: Array<[string, Array<string>]>;
    deleteDate?: number;
    disappearanceDate?: number;
    mergedWith?: string;
};

export type ListSearchParameters = {
    structureId?:String;
    profiles?:Array<Profile>;
    sortOn?: string;
    fromIndex?: number;
    limitResult?: number;
    searchType?: SearchTypeEnum;
    searchTerm?: string;
};

@Injectable()
export class UnlinkedUserService {
    constructor( private notify:NotifyService ) {
    }

    public list(params?:ListSearchParameters) {
        let url = `/directory/list/isolated?1=1`;
        if( params ) {
            if( params.structureId && params.structureId.length>0 ) 
                url += `&structureId=${params.structureId}`;
            if( params.profiles && params.profiles.length>0 ) 
                url += "&profile="+params.profiles.join("&profile=");
            if( params.sortOn && typeof params.fromIndex==="number" )
                url += `&sortOn=${encodeURIComponent(params.sortOn)}&fromIndex=${params.fromIndex}`;
            if( typeof params.limitResult==="number" )
                url += `&limitResult=${params.limitResult}`;
            if( params.searchType && params.searchTerm )
                url += `&searchType=${params.searchType}&searchTerm=${encodeURIComponent(params.searchTerm)}`;
        }
        return http.get<UnlinkedUser[]>(url)
            .then( response => {
                if( response.status===200 ) return response.data;
                throw response.statusText;
            })
            .catch( () => {
                this.notify.error("notify.structure.syncusers.error.content", "notify.structure.syncusers.error.title");
                return [] as Array<UnlinkedUser>;
            });
    }

    public fetch(userId: string): Promise<UnlinkedUserDetails|null> {
        return http.get<UnlinkedUserDetails>(`/directory/user/${userId}`)
            .then( response => {
                if( response.status===200 ) return response.data;
                throw response.statusText;
            })
            .catch( () => {
                this.notify.error("user.root.error", "user.root.error.text");
                return null;
            });
    }

    public delete(user: UnlinkedUserDetails): Promise<any> {
        return http.delete(`/directory/user?userId=${user.id}`)
        .then( response => {
            if( response.status===200 ) return response.data;
            throw response.statusText;
        });
    }

    public restore(user: UnlinkedUserDetails): Promise<any> {
        return http.put(`/directory/restore/user?userId=${user.id}`)
        .then( response => {
            if( response.status===200 ) {
                delete user.deleteDate;
                delete user.disappearanceDate;
                return response.data;
            }
            throw response.statusText;
        });
    }

    public update(userId:string, user:any) {
        return http.put(`/directory/user/${userId}`, user)
        .then( response => {
            if( response.status===200 ) return response.data;
            throw response.statusText;
        });
    }

    public addStructure(user:UnlinkedUserDetails, structureId:string) {
        return http.put(`/directory/structure/${structureId}/link/${user.id}`)
            .then( response => {
                if( response.status===200 ) return response.data;
                throw response.statusText;
            });
    }

    public removeStructure(user:UnlinkedUserDetails, structureId:string) {
        return http.delete(`/directory/structure/${structureId}/unlink/${user.id}`)
        .then( response => {
            if( response.status===200 ) return response.data;
            throw response.statusText;
        });
    }

    public getMergedWithDetails(userId: string): Promise<string|null> {
        return http.get<BackendDirectoryUserResponse>(`/directory/user/${userId}`)
            .then( response => {
                if( response.status===200 ) return response.data;
                throw response.statusText;
            })
            .then( response => `${response.displayName} (${response.login})` )
            .catch( () => {
                this.notify.error("user.root.error", "user.root.error.text");
                return null;
            });
    }

    public unmerge(userId:string, mergedLogin:string): Promise<Array<string>> {
        const payload = {
          originalUserId: userId,
          mergedLogins: [mergedLogin]
        }
        return http.post<{mergedLogins:Array<string>}>('/directory/duplicate/user/unmergeByLogins', payload)
            .then( response => {
                if( response.status===200 ) return response.data;
                throw response.statusText;
            })
            .then( result => {
              this.notify.success({
                key: 'notify.user.unmerge.content',
                parameters: {mergedLogin: mergedLogin}
              }, 'notify.user.unmerge.title');
              return result.mergedLogins;
            })
            .catch( err => {
              this.notify.error({
                key: 'notify.user.unmerge.error.content',
                parameters: {mergedLogin: mergedLogin}
              }, 'notify.user.unmerge.error.title', err);
              throw err;
            });
      }
    

}
