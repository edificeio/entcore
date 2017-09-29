import { RoleModel } from '.'
import { RoleCollection, globalStore } from '../..'

import { Model, Mix } from 'entcore-toolkit'

export class ApplicationModel extends Model<ApplicationModel> {

    constructor() {
        super({})
        this.roles = new Array<RoleModel>()
    }

    private _id: string

    get id(){ return this._id }
    set id(id) {
        this._id = id
    }

    syncRoles = (structureId: string, appId: string): Promise<void> => {
        return this.http.get(`/admin/api/structure/${structureId}/application/${appId}`)
            .then(res => {
                let roles = res.data
    
                this.roles = Mix.castArrayAs(RoleModel, roles)
                this.roles.forEach((role, index) => {
                    role.groups = new Map<string, string>(roles[index].groups.map(group => [group.id, group.name]))
                }) 
            }
        )
    }

    roles: RoleModel[]
}
