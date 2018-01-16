import { ConnectorModel } from '..';
import { Collection, Mix } from 'entcore-toolkit';

export class ConnectorCollection extends Collection<ConnectorModel> {

    constructor(){
        super({});
    }

    syncConnectors = () => {
        return this.http.get(`/appregistry/external-applications?structureId=${this.structureId}`)
            .then(res => {
                let connectors = new Array();
                res.data.forEach(connector => {
                    connectors.push({
                        id: connector.data.id,
                        name: connector.data.displayName,
                        icon: connector.data.icon
                    })
                });
                this.data = Mix.castArrayAs(ConnectorModel, connectors);

            })
    }
    
    public structureId : string;
}