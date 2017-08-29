import { ApplicationModel } from '..'
import { Collection } from 'entcore-toolkit'

export class ApplicationCollection extends Collection<ApplicationModel> {

    constructor(){
        super({
            sync: '/appregistry/applications'
        }, ApplicationModel)
        
    }

    public structureId : string

}