import { AbstractStore, StructureModel, UserModel } from '../core/store'

export class UsersStore extends AbstractStore {

    constructor(){ super(['structure', 'user']) }

    structure: StructureModel
    user: UserModel
}