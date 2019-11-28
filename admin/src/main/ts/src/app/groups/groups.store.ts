import { AbstractStore } from '../core/store/abstract.store';
import { StructureModel } from '../core/store/models/structure.model';
import { GroupModel } from '../core/store/models/group.model';

export class GroupsStore extends AbstractStore {

    constructor() {
        super(['structure', 'group']);
    }

    structure: StructureModel;
    group: GroupModel;
}
