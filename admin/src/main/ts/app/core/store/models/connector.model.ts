import { RoleModel } from '.';
import { Model, Mix } from 'entcore-toolkit';

export class ConnectorModel extends Model<ConnectorModel> {
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
    url: string;
    target: string;
    inherits: boolean;
    locked: boolean;

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

    syncRoles = (structureId: string, connectorId: string): Promise<void> => {
        return this.http.get(`/appregistry/application/external/${connectorId}/groups/roles?structureId=${structureId}`)
            .then(res => {
                this.roles = Mix.castArrayAs(RoleModel, res.data);
            }
        );
    }
}
