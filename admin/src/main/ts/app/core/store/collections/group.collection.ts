import { Collection } from 'entcore-toolkit'
import { GroupModel } from '..'

export class GroupCollection extends Collection<GroupModel> {

    constructor(){
        super({
            sync: '/directory/group/admin/list?structureId=:structureId&translate=:translate'
        }, GroupModel)
    }

    structureId: string
    translate: boolean = true
}