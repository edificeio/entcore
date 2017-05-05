import { Serializable } from './serializable'
import { UserDetailsModel } from '../userdetails.model'

export interface User {

    id: string
    type: string
    code: string
    login: string
    firstName: string
    lastName: string
    displayName: string
    source: string
    blocked: boolean
    aafFunctions: string[]
    structures: { id: string, name: string }[]
    classes: { id: string, name: string}[]
    duplicates: { id: string, firstName: string, lastName: string, code: string, structures: string[] }[]

    userDetails: UserDetailsModel

}