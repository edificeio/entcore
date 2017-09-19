import { Model } from 'entcore-toolkit'

export class RoleModel extends Model<RoleModel> {

    constructor() {
        super({})
    }
    
    roleId: string
    roleName: string
    groups: [{
        groupId: string, 
        groupName: string
    }]
}