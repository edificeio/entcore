import { AbstractDataService } from '../../../services/abstract.data.service'
import { StructureModel } from '../../../store/structure.model'
import { UserModel } from '../../../store/user.model'

export class UsersDataService extends AbstractDataService {

    constructor(){ super(['structure', 'user']) }

    structure: StructureModel
    user: UserModel

}