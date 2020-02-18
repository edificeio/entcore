import {Collection} from 'entcore-toolkit'
import {SubjectModel} from '../models/subject.model';

export class SubjectCollection extends Collection<SubjectModel> {

    constructor() {
        super({
            sync: '/directory/subject/admin/list?structureId=:structureId'
        }, SubjectModel)
    }

    structureId: string
}