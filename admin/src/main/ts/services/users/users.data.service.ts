import { AbstractDataService } from '../abstract.data.service'
import { StructureModel } from '../../models/structure.model'
import { UserModel } from '../../models/user.model'

export class UsersDataService extends AbstractDataService {

    constructor(){ super(['structure', 'user']) }

    structure: StructureModel
    user: UserModel

}