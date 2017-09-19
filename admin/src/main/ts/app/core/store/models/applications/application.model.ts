import { RoleModel, ApplicationDetailsModel } from '.'
import { RoleCollection, globalStore } from '../..'

import { Model } from 'entcore-toolkit'

export class ApplicationModel extends Model<ApplicationModel> {

    constructor() {
        super({})
        this.details = new ApplicationDetailsModel()
        this.roles = new Array<RoleModel>()
    }

    private _id: string

    get id(){ return this._id }
    set id(id) {
        this._id = id
        this.details.id = id
    }

    syncRoles = (structureId: string, appId: string): Promise<void> => {
        return this.http.get(`/admin/api/structure/${structureId}/application/${appId}`)
            .then(res => {
                this.roles = res.data
            })
    }

    details: ApplicationDetailsModel
    roles: RoleModel[]
}
