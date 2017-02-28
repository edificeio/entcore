import { AbstractStore, StructureModel, Group } from '../../../store'

export class GroupsStore extends AbstractStore {

    constructor(){ super(['structure', 'group']) }

    structure: StructureModel
    group: Group

}