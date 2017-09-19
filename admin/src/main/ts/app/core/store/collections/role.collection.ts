import { RoleModel } from '..'
import { Collection } from 'entcore-toolkit'

export class RoleCollection extends Collection<RoleModel> {

    constructor(){
        super({
            sync: '/admin/api/structure/:structureId/application/:appId'
        }, RoleModel)
    }

    structureId: string
}