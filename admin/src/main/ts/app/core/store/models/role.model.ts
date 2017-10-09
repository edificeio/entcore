import { Model } from 'entcore-toolkit';

export class RoleModel extends Model<RoleModel> {

    constructor() {
        super({});
    }
    
    id: string;
    name: string;
    groups: Map<string, string>;
    transverse: boolean

    removeGroupFromRole = (groupId: string): Promise<void> => {
        return this.http
            .delete(`/appregistry/authorize/group/${groupId}/role/${this.id}`)
            .then((res) => { 
                this.groups.delete(groupId);
                this.groups = new Map(this.groups.entries());
            })
            .catch(e => console.log(e)
        );
    }

    addGroupsToRole = (groups) => {
        this.groups = new Map(this.groups.entries());
        groups.forEach((g) => this.groups.set(g.id, g.name));
    }
}