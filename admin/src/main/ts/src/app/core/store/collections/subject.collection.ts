import {Collection} from 'entcore-toolkit'
import {SubjectModel} from '..'

export class SubjectCollection extends Collection<SubjectModel> {

    constructor() {
        super({
            sync: '/directory/subject/admin/list?structureId=:structureId'
        }, SubjectModel)
    }

    structureId: string
}