import { RoleModel } from '.';
import { Model, Mix } from 'entcore-toolkit';

export class ConnectorModel extends Model<ConnectorModel> {

    constructor() {
        super({});
        this.roles = [];
    }

    private _id: string;

    get id(){ return this._id };
    set id(id) {
        this._id = id;
    };

    syncRoles = (structureId: string, connectorId: string): Promise<void> => {
        return this.http.get(`/appregistry/application/external/${connectorId}/groups/roles?structureId=${structureId}`)
            .then(res => {
                this.roles = Mix.castArrayAs(RoleModel, res.data);
            }
        );
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
    casTypeId: string;
    casPattern: string;
    
    oauthTransferSession: boolean;
    oauthScope: string;
    oauthSecret: string;
    oauthGrantType: string;
}
