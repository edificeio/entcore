import { Group } from '../../models/mappings/group'
import { AbstractDataService } from '../abstract.data.service'
import { StructureModel } from '../../models/structure.model'

export class GroupsDataService extends AbstractDataService {

    constructor(){ super(['structure', 'group']) }

    structure: StructureModel
    group: Group

}