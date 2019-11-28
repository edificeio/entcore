import {Collection, Mix} from 'entcore-toolkit';
import { ApplicationModel } from '../models/application.model';

export class ApplicationCollection extends Collection<ApplicationModel> {

    constructor() {
        super({});
    }

    public structureId: string;

    syncApps = (structureId: string) => {
        return this.http.get(`/appregistry/applications/roles?structureId=${structureId}`)
            .then(
                res => {
                    this.data = Mix.castArrayAs(ApplicationModel,
                        res.data.reduce((apps, item) => {
                            if (!item.isExternal) { apps.push(item); }
                            return apps;
                        }, [])
                    );
                }
            );
    }
}
