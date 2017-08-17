import { UserModel } from '..'
import { Collection } from 'entcore-toolkit'

export class UserCollection extends Collection<UserModel> {

    constructor(){
        super({
            //sync: '/directory/user/admin/list?structureId=:structureId'
            sync: '/admin/api/structure/:structureId/users'
        }, UserModel)
    }

    public structureId : string

}