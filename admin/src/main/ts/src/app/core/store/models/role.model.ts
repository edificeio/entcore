import {Model} from 'entcore-toolkit';
import {GroupModel} from './group.model';
import { WidgetModel } from './widget.model';

export type RoleActionModel = {
    name: string;
    displayName: string;
    type: string;
}

export class RoleModel extends Model<RoleModel> {

    constructor() {
        super({});
    }

    id: string;
    name: string;
    groups: GroupModel[];
    transverse: boolean;
    subStructures: string[];
    distributions: string[];

    removeGroup(group: GroupModel): Promise<void> {
        return this.http
            .delete(`/appregistry/authorize/group/${group.id}/role/${this.id}`)
            .then((res) => {
                const groupIndex = this.groups.findIndex(g => g.id == group.id);
                this.groups.splice(groupIndex, 1);
            })
            .catch(e => console.error(e)
        );
    }

    async addGroup(group: GroupModel) {
        const res = await this.http.put(`/appregistry/authorize/group/${group.id}/role/${this.id}`);
        this.groups.push(group);
        return res;
    }
}
