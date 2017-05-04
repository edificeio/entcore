import { Model } from 'toolkit'

export type GroupUser = {
    id: string,
    firstName: string,
    lastName: string,
    type: string,
    profile: string,
    login: string,
    username: string,
    structures: {id: string, name: string}[]
}

export class GroupModel extends Model<GroupModel> {

    id?: string
    name?: string
    displayName?: string
    type?: string
    classes?: {id: string, name: string}[]
    structureId?: string
    users: GroupUser[]

    constructor() {
        super({
            create: '/directory/group'
        })
        this.users = new Array<GroupUser>()
    }

    syncUsers() {
        return this.http.get(`/directory/user/group/${this.id}`).then(res => {
            this.users = res.data
        })
    }

    toJSON() {
        return {
            name: this.name,
            structureId: this.structureId
        }
    }
}