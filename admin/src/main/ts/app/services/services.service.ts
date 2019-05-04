import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { ConnectorModel } from "../core/store";
import { CasType } from "./connectors/CasType";

import 'rxjs/add/operator/share';

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
                inherits: connector.inherits,
                appLocked: connector.locked,
                casType: connector.casTypeId || '',
                pattern: connector.casPattern || '',
                scope: connector.oauthScope || '',
                secret: connector.oauthSecret || '',
                grantType: connector.oauthGrantType || '',
            })
    }

    public getCasTypes(): Observable<CasType[]> {
        return this.httpClient.get<CasType[]>('/appregistry/cas-types');
    }
}