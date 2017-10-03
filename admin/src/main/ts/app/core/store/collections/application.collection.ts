import { ApplicationModel } from '..';
import { Collection, Mix } from 'entcore-toolkit';

export class ApplicationCollection extends Collection<ApplicationModel> {

    constructor(){
        super({});
    }

    syncApps = () => {
        return this.http.get('/appregistry/applications')
            .then(res => this.data = Mix.castArrayAs(ApplicationModel, res.data));
    }
    
    public structureId : string;
}