import { Model } from 'entcore-toolkit'

export class RoleModel extends Model<RoleModel> {

    constructor() {
        super({})
    }
    
    id: string
    name: string
    groups: Map<string, string>

    removeGroupLink = (groupId: string, roleId: string): Promise<void> => {
        return this.http
            .delete(`/appregistry/authorize/group/${groupId}/role/${roleId}`)
            .then((res) => { 
                this.groups.delete(groupId)
                this.groups = new Map(this.groups.entries()) 
            })
            .catch(e => console.log(e))
    }
}