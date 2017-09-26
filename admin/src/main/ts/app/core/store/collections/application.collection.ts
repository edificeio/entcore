import { ApplicationModel } from '..'
import { Collection } from 'entcore-toolkit'

export class ApplicationCollection extends Collection<ApplicationModel> {

    constructor(){
        super({})
    }

    syncApps = () => {
        return this.http.get(`/appregistry/applications-icons?structureId=${this.structureId}`)
            .then(res => {
                let apps = res.data.map(app => Object.assign(new ApplicationModel, app))
                this.data = apps
            })
    }
    public structureId : string
}