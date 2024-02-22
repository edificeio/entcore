import {Mix, Model} from 'entcore-toolkit';
import { RoleModel } from './role.model';

export interface IConnector{
    id: string;
    name: string;
    displayName: string;
    icon: string;
    url: string;
    target: string;
    inherits: boolean;
    locked: boolean;
    casTypeId: string;
    statCasType: string;
    casPattern: string;
    oauthScope: string;
    oauthSecret: string;
    logoutUrl: string;
    oauthGrantType: string;
    structureId: string;
    oauthTransferSession: boolean;
}

export class MappingModel {
    type: string;
    pattern: string;
    casType: string;
    xitiOutil: string;
    xitiService: string;
    connectorsInStruct?: string[] = [];
    connectorsOutsideStruct?:number;
    get id(){
        return this.type;
    }
    get name(){
        return this.type;
    }
}

export class ConnectorModel extends Model<ConnectorModel> implements IConnector{
    private _id: string;
    public get id() {
        return this._id;
    }
    public set id(id) {
        this._id = id;
    }

    private _casTypeId: string;
    get casTypeId() {
        return this._casTypeId;
    }
    set casTypeId(casTypeId) {
        this._casTypeId = casTypeId;
        if (casTypeId) {
            this.hasCas = true;
        }
    }

    name: string;
    displayName: string;
    icon: string;
    iconFile: File | Blob;
    url: string;
    target: string;
    inherits: boolean;
    locked: boolean;
    statCasType: string;
    roles: RoleModel[];

    hasCas: boolean;
    casPattern: string;

    oauthTransferSession: boolean;
    oauthScope: string;
    oauthSecret: string;
    oauthGrantType: string;

    structureId: string;

    constructor() {
        super({});
        this.roles = [];
    }
    logoutUrl: string;

    syncRoles = (structureId: string, connectorId: string): Promise<void> => {
        return this.http.get(`/appregistry/application/external/${connectorId}/groups/roles?structureId=${structureId}`)
            .then(res => {
                this.roles = Mix.castArrayAs(RoleModel, res.data);
            }
        );
    }
}
