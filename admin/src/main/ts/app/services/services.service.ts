import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { ConnectorModel } from "../core/store";
import { CasType } from "./connectors/connector/CasType";
import { Profile } from "./shared/services-types";
import { ExportFormat } from "./connectors/connector/export/connector-export";

import 'rxjs/add/operator/do';

export interface Document {
    _id: string;
    ancestors: Array<any>;
    application: string;
    created: Date;
    eParent: any;
    eType: string;
    file: string;
    inheritedShares: Array<any>;
    isShared: boolean;
    metadata: any;
    modified: Date;
    name: string;
    nameSearch: string;
    owner: string;
    ownerName: string;
    public: boolean;
    shared: Array<any>;
}

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

    public getExportConnectorUrl(exportFormat: ExportFormat, profile: string, structureId: string): string {
        const query = '/directory/export/users';
        let queryParams = `format=${exportFormat.format}&structureId=${structureId}&type=${exportFormat.value}`

        if (profile !== 'all') {
            queryParams = `${queryParams}&profile=${profile}`;
        }

        return `${query}?${queryParams}`;
    }

    public isIconImage(connector: ConnectorModel): boolean {
        return connector 
            && connector.icon 
            && (connector.icon.startsWith('/workspace') || connector.icon.startsWith("http"));
    }

    public uploadPublicImage(image: File | Blob): Observable<Document> {
        let formData: FormData = new FormData();
        formData.append('file', image);
        
        let queryParams: string[] = [];
        queryParams.push('public=true');
        queryParams.push('application=admin');
        queryParams.push('quality=0.7');
        queryParams.push('thumbnail=120x120&thumbnail=150x150&thumbnail=100x100&thumbnail=290x290&thumbnail=48x48&thumbnail=82x82&thumbnail=381x381');

        return this.httpClient.post<Document>(`/workspace/document?${queryParams.join('&')}`, formData);
    }
}
