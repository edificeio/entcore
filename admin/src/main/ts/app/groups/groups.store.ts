import { AbstractStore, GroupModel, StructureModel } from '../core/store';

export class GroupsStore extends AbstractStore {

    constructor() {
        super(['structure', 'group']);
    }

    structure: StructureModel;
    group: GroupModel;
}
