import { Model } from 'entcore-toolkit'

export class RoleModel extends Model<RoleModel> {

    constructor() {
        super({})
    }
    
    id: string
    name: string
    groups: Map<string, string>
}