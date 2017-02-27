import { AbstractDataService } from '../../../services/abstract.data.service'
import { Group } from '../../../store/mappings/group'
import { StructureModel } from '../../../store/structure.model'

export class GroupsDataService extends AbstractDataService {

    constructor(){ super(['structure', 'group']) }

    structure: StructureModel
    group: Group

}