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

    removeGroup(group:GroupModel): Promise<void> {
        return this.http
            .delete(`/appregistry/authorize/group/${group.id}/role/${this.id}`)
            .then((res) => { 
                let groupIndex = this.groups.findIndex(g => {return g.id == group.id})
                this.groups.splice(groupIndex,1);
            })
            .catch(e => console.log(e)
        );
    }

    async addGroup(group:GroupModel) {
        let res = await this.http.put(`/appregistry/authorize/group/${group.id}/role/${this.id}`);
        this.groups.push(group);
        return res;
    }
}