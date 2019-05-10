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
                        name: connector.data.name,
                        displayName: connector.data.displayName,
                        icon: connector.data.icon,
                        url: connector.data.address,
                        target: connector.data.target,
                        inherits: connector.data.inherits,
                        locked: connector.data.locked,
                        casTypeId: connector.data.casType,
                        casPattern: connector.data.pattern,
                        oauthScope: connector.data.scope,
                        oauthSecret: connector.data.secret,
                        oauthGrantType: connector.data.grantType,
                        structureId: connector.data.structureId,
                        oauthTransferSession: this.setOauthTransferSession(connector.data.scope)
                    })
                });
                this.data = Mix.castArrayAs(ConnectorModel, connectors);
            })
    }

    private setOauthTransferSession(scope: string): boolean {
        return scope && scope.startsWith('userinfo');
    }
    
    public structureId : string;
}