import { UserModel } from '..'
import { Collection } from 'entcore-toolkit'

export class UserCollection extends Collection<UserModel> {

    constructor(){
        super({
            sync: '/directory/structure/:structureId/users'
        }, UserModel)
    }

    public structureId : string

}