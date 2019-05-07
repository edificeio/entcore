import { RoleModel } from '.';

import { Mix, Model } from 'entcore-toolkit';

export type AppType = 'END_USER' | 'SYSTEM' | 'WIDGET';

export class ApplicationModel extends Model<ApplicationModel> {

    constructor() {
        super({});
        this.roles = [];
    }

    private _id: string;

    get id() {
        return this._id
    };

    set id(id) {
        this._id = id
    };

    syncRoles = (structureId: string): Promise<void> => {
        return this.http.get(`/appregistry/structure/${structureId}/application/${this._id}/groups/roles`)
            .then(res => {
                    this.roles = Mix.castArrayAs(RoleModel, res.data);
                }
            );
    };

    private _name: string;
    get name() {
        return this._name;
    }
    set name(name) {
        this._name = name;
        this.displayName = name;
    }

    displayName: string;
    roles: RoleModel[];
    levelsOfEducation: number[];
    appType: AppType;
    icon: string;
    isExternal: boolean;
}
