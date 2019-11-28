import {Mix, Model} from 'entcore-toolkit';
import { RoleModel } from './role.model';

export type AppType = 'END_USER' | 'SYSTEM' | 'WIDGET';

export class ApplicationModel extends Model<ApplicationModel> {

    id: string;
    name: string;
    displayName: string;
    roles: RoleModel[];
    levelsOfEducation: number[];
    appType: AppType;
    icon: string;
    isExternal: boolean;

    constructor() {
        super({});
        this.roles = [];
    }

    syncRoles = (structureId: string): Promise<void> => {
        return this.http.get(`/appregistry/structure/${structureId}/application/${this.id}/groups/roles`)
            .then(res => {
                    this.roles = Mix.castArrayAs(RoleModel, res.data);
                }
            );
    }
}
