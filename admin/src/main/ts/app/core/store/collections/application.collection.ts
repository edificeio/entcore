import { ApplicationModel } from '..';
import { Collection, Mix } from 'entcore-toolkit';

export class ApplicationCollection extends Collection<ApplicationModel> {

    constructor(){
        super({});
    }

    syncApps = (structureId:string) => {
        return this.http.get(`/appregistry/applications?structureId=${structureId}`)
            .then(
                res => {
                    this.data = Mix.castArrayAs(ApplicationModel, 
                        res.data.reduce((apps, item) => {
                            if (!item.isExternal) apps.push(item);
                            return apps;
                        },[])
                    );
                }
            );
    }
    
    public structureId : string;
}