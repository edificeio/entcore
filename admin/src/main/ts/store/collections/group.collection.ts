import { Collection } from 'toolkit'
import { GroupModel } from '..'

export class GroupCollection extends Collection<GroupModel> {

    constructor(){
        super({
            sync: '/directory/group/admin/list?structureId=:structureId'
        }, GroupModel)
    }

    structureId: string
}