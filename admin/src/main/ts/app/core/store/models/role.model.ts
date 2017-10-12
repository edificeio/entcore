import { Model } from 'entcore-toolkit';
import { GroupModel } from './group.model';

export class RoleModel extends Model<RoleModel> {

    constructor() {
        super({});
    }
    
    id: string;
    name: string;
    groups: GroupModel[];
    transverse: boolean

    removeGroupFromRole = (group:GroupModel): Promise<void> => {
        return this.http
            .delete(`/appregistry/authorize/group/${group.id}/role/${this.id}`)
            .then((res) => { 
                let groupIndex = this.groups.findIndex(g => {return g.id == group.id})
                this.groups.splice(groupIndex,1);
            })
            .catch(e => console.log(e)
        );
    }

    addGroupsToRole = (groups) => {
        this.groups = groups;
    }
}