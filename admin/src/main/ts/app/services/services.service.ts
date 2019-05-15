import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { ConnectorModel, GroupModel, RoleModel } from "../core/store";
import { CasType } from "./connectors/connector/CasType";

import 'rxjs/add/operator/do';
import { MassAssignment, Profile } from "./shared/assignment-types";

@Injectable()
export class ServicesService {

    constructor(private httpClient: HttpClient) {
    }

    public createConnector(connector: ConnectorModel, structureId: string): Observable<{id: string, roleId: string}> {
        if(connector.target === 'adapter' && connector.url.indexOf('/adapter#') === -1){
            connector.target = '';
            connector.url = '/adapter#' + connector.url;
        }

        return this.httpClient.post<{ id: string, roleId: string }>(
            `/appregistry/application/external?structureId=${structureId}`
            , {
                name: connector.name,
                displayName: connector.displayName,
                icon: connector.icon || '',
                address: connector.url,
                target: connector.target || '',
                inherits: connector.inherits || false,
                appLocked: connector.locked || false,
                casType: connector.casTypeId || '',
                pattern: connector.casPattern || '',
                scope: connector.oauthScope || '',
                secret: connector.oauthSecret || '',
                grantType: connector.oauthGrantType || ''
            })
    }

    public saveConnector(connector: ConnectorModel, structureId: string): Observable<{id: string}>{
        return this.httpClient.put<{id: string}>(
            `/appregistry/application/conf/${connector.id}?structureId=${structureId}`
            , {
                name: connector.name,
                displayName: connector.displayName,
                icon: connector.icon || '',
                address: connector.url,
                target: connector.target || '',
                inherits: connector.inherits || false,
                appLocked: connector.locked || false,
                casType: connector.casTypeId || '',
                pattern: connector.casPattern || '',
                scope: connector.oauthScope || '',
                secret: connector.oauthSecret || '',
                grantType: connector.oauthGrantType || ''
            });
    }

    public deleteConnector(connector: ConnectorModel): Observable<void> {
        return this.httpClient.delete<void>(`/appregistry/application/external/${connector.id}`);
    }

    public toggleLockConnector(connector: ConnectorModel): Observable<void> {
        return this.httpClient.put<void>(`/appregistry/application/external/${connector.id}/lock`, {});
    }

    public getCasTypes(): Observable<CasType[]> {
        return this.httpClient.get<CasType[]>('/appregistry/cas-types');
    }

    public massAssignConnector(connector: ConnectorModel, profiles: Array<Profile>): Observable<void> {
        var profilesParams = "";

        profiles.forEach(p => {
            if (profilesParams) {
                profilesParams += "&profile=" + p;
            } else {
                profilesParams += "?profile=" + p;
            }
        });

        return this.httpClient.put<void>(`/appregistry/application/external/${connector.id}/authorize${profilesParams}`, {});
    }

    public massUnassignConnector(connector: ConnectorModel, profiles: Array<Profile>): Observable<void> {
        var profilesParams = "";

        profiles.forEach(p => {
            if (profilesParams) {
                profilesParams += "&profile=" + p;
            } else {
                profilesParams += "?profile=" + p;
            }
        });

        return this.httpClient.delete<void>(`/appregistry/application/external/${connector.id}/authorize${profilesParams}`, {});
    }
}