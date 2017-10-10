import { RoleModel } from '.';
import { RoleCollection, globalStore } from '..';

import { Model, Mix } from 'entcore-toolkit';

export class ConnectorModel extends Model<ConnectorModel> {

    constructor() {
        super({});
        this.roles = new Array<RoleModel>();
    }

    private _id: string;

    get id(){ return this._id };
    set id(id) {
        this._id = id;
    };

    syncRoles = (structureId: string, connectorId: string): Promise<void> => {
        return this.http.get(`/appregistry/application/external/${connectorId}/groups/roles?structureId=${structureId}`)
            .then(res => {
                let roles = res.data;
    
                this.roles = Mix.castArrayAs(RoleModel, roles);
                this.roles.forEach((role, index) => {
                    role.groups = new Map<string, string>(roles[index].groups.map(group => [group.id, group.name]))
                }) 
            }
        );
    }

    name: string;
    icon: string;
    roles: RoleModel[];
}
