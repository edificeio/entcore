import {Collection} from 'entcore-toolkit';
import {GroupModel} from '../models/group.model';

export class GroupCollection extends Collection<GroupModel> {

    constructor() {
        super({
            sync: '/directory/group/admin/list?structureId=:structureId&translate=:translate'
        }, GroupModel);
    }

    structureId: string;
    translate = true;
}
