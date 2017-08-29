import { AppActionModel } from './application-action.model';
import { AppActionsCollection } from '../../collections/app-actions.collection';
import { Model } from 'entcore-toolkit'
import { ApplicationDetailsModel } from './applicationdetails.model'
import { globalStore } from '../..'

export class ApplicationModel extends Model<ApplicationModel> {

    constructor() {
        super({})
        this.applicationDetails = new ApplicationDetailsModel()
        this.applicationActions = new AppActionModel
    }

    private _id: string
    get id(){ return this._id }
    set id(id) {
        this._id = id
        this.applicationDetails.id = id
    }

    applicationDetails: ApplicationDetailsModel
    applicationActions: AppActionModel
}
