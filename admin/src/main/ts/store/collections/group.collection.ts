import { Collection } from 'toolkit'
import { Group } from '..'

export class GroupCollection extends Collection<Group> {

    constructor(){
        super({
            sync: '/directory/group/admin/list?structureId=:structureId'
        }, Group)
    }

    structureId: string
}