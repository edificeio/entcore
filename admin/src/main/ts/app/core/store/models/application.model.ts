import { RoleModel } from '.';
import { RoleCollection, globalStore } from '..';

import { Model, Mix } from 'entcore-toolkit';

export class ApplicationModel extends Model<ApplicationModel> {

    constructor() {
        super({});
        this.roles = [];
    }

    private _id: string;

    get id(){ return this._id };
    set id(id) {
        this._id = id
    };

    syncRoles = (structureId: string): Promise<void> => {
        return this.http.get(`/appregistry/structure/${structureId}/application/${this._id}/groups/roles`)
            .then(res => {
                this.roles = Mix.castArrayAs(RoleModel, res.data);
            }
        );
    }

    roles: RoleModel[];
}
