import { Model } from 'entcore-toolkit'
import { globalStore } from '../..'

export class AppActionModel extends Model<AppActionModel> {

    constructor() {
        super({})
    }

    id: string
}