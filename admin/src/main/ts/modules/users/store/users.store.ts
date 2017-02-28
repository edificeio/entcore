import { AbstractStore, StructureModel, UserModel } from '../../../store'

export class UsersStore extends AbstractStore {

    constructor(){ super(['structure', 'user']) }

    structure: StructureModel
    user: UserModel

}