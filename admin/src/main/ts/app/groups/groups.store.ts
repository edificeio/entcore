import { AbstractStore, StructureModel, GroupModel } from '../core/store'

export class GroupsStore extends AbstractStore {

    constructor(){ super(['structure', 'group']) }

    structure: StructureModel
    group: GroupModel
}