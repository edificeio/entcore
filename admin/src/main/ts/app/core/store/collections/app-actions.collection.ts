import { AppActionModel } from '..'
import { Collection } from 'entcore-toolkit'

export class AppActionsCollection extends Collection<AppActionModel> {

    constructor(){
        super({
            sync: '/appregistry/applications/actions?actionType=WORKFLOW&structureId=:structureId'
        }, AppActionModel)
    }

    structureId: string
}