import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConnectorModel } from '../core/store/models/connector.model';
import { CasType } from './connectors/connector/CasType';
import { Profile } from './_shared/services-types';
import { ExportFormat } from './connectors/connector/export/connector-export.component';


@Injectable()
export class ServicesService {

    constructor(private httpClient: HttpClient) {
    }

    public createConnector(connector: ConnectorModel, structureId: string): Observable<{ id: string, roleId: string }> {
        if (connector.target === 'adapter' && connector.url.indexOf('/adapter#') === -1) {
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
                statCasType: connector.statCasType,
                target: connector.target || '',
                inherits: connector.inherits || false,
                casType: connector.casTypeId || '',
                pattern: connector.casPattern || '',
                logoutUrl: connector.logoutUrl || '',
                scope: connector.oauthScope || '',
                secret: connector.oauthSecret || '',
                grantType: connector.oauthGrantType || '',
                certUri: connector.oauthCertUri || ''
            });
    }

    public saveConnector(connector: ConnectorModel, structureId: string): Observable<{ id: string }> {
        return this.httpClient.put<{ id: string }>(
            `/appregistry/application/conf/${connector.id}?structureId=${structureId}`
            , {
                name: connector.name,
                displayName: connector.displayName,
                icon: connector.icon || '',
                address: connector.url,
                statCasType: connector.statCasType,
                target: connector.target || '',
                inherits: connector.inherits || false,
                casType: connector.casTypeId || '',
                pattern: connector.casPattern || '',
                logoutUrl: connector.logoutUrl || '',
                scope: connector.oauthScope || '',
                secret: connector.oauthSecret || '',
                grantType: connector.oauthGrantType || '',
                certUri: connector.oauthCertUri || ''
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

    public massAssignConnector(structureId: string, connector: ConnectorModel, profiles: Array<Profile>): Observable<void> {
        let profilesParams = '';

        profiles.forEach(p => {
            if (profilesParams) {
                profilesParams += '&profile=' + p;
            } else {
                profilesParams += '?profile=' + p;
            }
        });

        return this.httpClient.put<void>(`/appregistry/structure/${structureId}/application/external/${connector.id}/authorize${profilesParams}`, {});
    }

    public massUnassignConnector(structureId: string, connector: ConnectorModel, profiles: Array<Profile>): Observable<void> {
        let profilesParams = '';

        profiles.forEach(p => {
            if (profilesParams) {
                profilesParams += '&profile=' + p;
            } else {
                profilesParams += '?profile=' + p;
            }
        });

        return this.httpClient.delete<void>(`/appregistry/structure/${structureId}/application/external/${connector.id}/authorize${profilesParams}`, {});
    }

    public getExportConnectorUrl(exportFormat: ExportFormat, profile: string, structureId: string): string {
        const query = '/directory/export/users';
        let queryParams = `format=${exportFormat.format}&structureId=${structureId}&type=${exportFormat.value}`;

        if (profile !== 'all') {
            queryParams = `${queryParams}&profile=${profile}`;
        }

        return `${query}?${queryParams}`;
    }

    public uploadPublicImage(image: File | Blob): Observable<WorkspaceDocument> {
        const formData: FormData = new FormData();
        formData.append('file', image);

        const queryParams: string[] = [];
        queryParams.push('public=true');
        queryParams.push('application=admin');
        queryParams.push('quality=0.7');
        return this.httpClient.post<WorkspaceDocument>(`/workspace/document?${queryParams.join('&')}`, formData);
    }
}

export interface WorkspaceDocument {
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
