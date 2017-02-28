import { UserModel } from '..'
import http from 'axios'

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

export class Group {
    id?: string
    name?: string
    displayName?: string
    type?: string
    classes?: {id: string, name: string}[]
    users: GroupUser[]

    syncUsers() {
        return http.get(`/directory/user/group/${this.id}`).then(res => {
            this.users = res.data
        })
    }
}