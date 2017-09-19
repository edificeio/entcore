import { Model } from 'entcore-toolkit'

export class ApplicationDetailsModel extends Model<ApplicationDetailsModel> {

    constructor() {
        super({
            sync: '/appregistry/application/conf/:id',
        })
    }

    id: string
    name: string
}