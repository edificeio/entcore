import {Injectable} from '@angular/core';
import http from 'axios';
import { Profile } from 'src/app/services/_shared/services-types';
import { NotifyService } from '../../../core/services/notify.service';
import { BackendDirectoryUserResponse } from 'src/app/users/users.service';

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
};

@Injectable()
export class UnlinkedUserService {
    constructor( private notify:NotifyService ) {
    }

    public list(structureId?:String, profiles?:Array<Profile>) {
        const url = `/directory/list/isolated?1=1`;
        return http.get<UnlinkedUser[]>(
                url
                + (structureId && structureId.length>0 ? `&structureId=${structureId}` : "")
                + (profiles && profiles.length>0 ? "&profile="+profiles.join("&profile=") : "")
            )
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
}
